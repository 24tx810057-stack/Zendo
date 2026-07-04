package com.zendo.apps.ui.activities;

import com.zendo.apps.utils.RecommendationEngine;

import com.zendo.apps.R;

import com.zendo.apps.utils.SharedPrefManager;

import com.zendo.apps.ui.adapters.HorizontalProductAdapter;

import com.zendo.apps.ui.adapters.ImageSliderAdapter;

import com.zendo.apps.data.models.CartItem;

import com.zendo.apps.data.models.Product;

import com.zendo.apps.data.models.Review;

import com.zendo.apps.data.models.User;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.text.style.UnderlineSpan;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.TranslateAnimation;
import android.view.animation.ScaleAnimation;
import android.widget.FrameLayout;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.widget.ViewPager2;

import com.zendo.apps.data.models.AuthResultState;
import com.bumptech.glide.Glide;
import com.google.android.material.chip.Chip;
import com.zendo.apps.viewmodels.ProductViewModel;
import com.zendo.apps.viewmodels.CartViewModel;
import com.zendo.apps.databinding.ActivityDetailBinding;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

public class DetailActivity extends AppCompatActivity {

    private ActivityDetailBinding binding;
    private ProductViewModel viewModel;
    private CartViewModel cartViewModel;
    private DecimalFormat formatter = new DecimalFormat("###,###,###");
    private String productId;
    private Product product;
    private String userEmail, userRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.TRANSPARENT);
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }

        binding = ActivityDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(ProductViewModel.class);
        cartViewModel = new ViewModelProvider(this).get(CartViewModel.class);
        SharedPrefManager prefManager = SharedPrefManager.getInstance(this);
        userEmail = prefManager.getUserEmail();
        userRole = prefManager.getUserRole();

        initViews();
        setupHeaderActions();
        
        product = (Product) getIntent().getSerializableExtra("product_data");
        if (product != null) {
            productId = product.getId();
            displayData();
            checkIfLiked();
        }

        binding.tvViewAllReviews.setOnClickListener(v -> {
            Intent intent = new Intent(this, ReviewListActivity.class);
            intent.putExtra("product_id", productId);
            startActivity(intent);
        });
    }

    private void initViews() {
        binding.layoutHeaderSpecs.setOnClickListener(v -> toggleCollapse(binding.layoutContentSpecs, binding.ivArrowSpecs));
        binding.layoutHeaderDesc.setOnClickListener(v -> toggleCollapse(binding.layoutContentDesc, binding.ivArrowDesc));

        if ("admin".equals(userRole)) {
            binding.layoutAdminActions.setVisibility(View.VISIBLE);
            binding.layoutUserActions.setVisibility(View.GONE);
        } else {
            binding.layoutAdminActions.setVisibility(View.GONE);
            binding.layoutUserActions.setVisibility(View.VISIBLE);
        }

        binding.btnEditProduct.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddProductActivity.class);
            intent.putExtra("edit_product", product);
            startActivity(intent);
        });
        
        binding.btnDeleteProduct.setOnClickListener(v -> showDeleteDialog());

        binding.btnBuyNow.setOnClickListener(v -> handleBuyNow());
        binding.btnAddToCart.setOnClickListener(v -> addToCart());
        binding.btnChatNowDetail.setOnClickListener(v -> openChat(false));
        binding.fabShopChat.setOnClickListener(v -> openChat(true));

        binding.ivFavorite.setOnClickListener(v -> toggleFavorite());

        binding.btnViewWarrantyPolicy.setOnClickListener(v -> {
            startActivity(new Intent(this, WarrantyPolicyActivity.class));
        });
    }

    private void openChat(boolean isBot) {
        String shopName = "Phone Store"; // Tên Shop cố định theo yêu cầu
        ChatBottomSheetFragment chatSheet = ChatBottomSheetFragment.newInstance(
                product != null ? product.getId() : "general",
                shopName,
                isBot
        );
        chatSheet.show(getSupportFragmentManager(), "ChatBottomSheet");
    }

    private void checkIfLiked() {
        if (product != null && userEmail != null) {
            if (product.getLikedBy() != null && product.getLikedBy().contains(userEmail)) {
                binding.ivFavorite.setImageResource(R.drawable.ic_heart_filled);
                binding.ivFavorite.setColorFilter(getResources().getColor(R.color.pink_heart));
            } else {
                binding.ivFavorite.setImageResource(R.drawable.ic_heart_outline);
                binding.ivFavorite.setColorFilter(getResources().getColor(R.color.gray_inactive));
            }
        }
    }

    private void toggleFavorite() {
        if (productId == null || userEmail == null || userEmail.isEmpty()) {
            Toast.makeText(this, "Vui lòng đăng nhập để thực hiện!", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean currentlyLiked = product.getLikedBy() != null && product.getLikedBy().contains(userEmail);

        if (currentlyLiked) {
            viewModel.toggleFavorite(productId, userEmail, true).observe(this, state -> {
                if (state.getStatus() == AuthResultState.Status.SUCCESS) {
                    product.getLikedBy().remove(userEmail);
                    checkIfLiked();
                    Toast.makeText(this, "Đã bỏ yêu thích", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            viewModel.toggleFavorite(productId, userEmail, false).observe(this, state -> {
                if (state.getStatus() == AuthResultState.Status.SUCCESS) {
                    if (product.getLikedBy() == null) product.setLikedBy(new ArrayList<>());
                    product.getLikedBy().add(userEmail);
                    checkIfLiked();
                    Toast.makeText(this, "Đã thêm vào yêu thích", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void setupHeaderActions() {
        binding.btnBackDetail.setOnClickListener(v -> finish());
        binding.btnShareProduct.setOnClickListener(v -> shareProduct());
        
        binding.btnMoreDetail.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(this, v);
            popup.getMenu().add("Quay về trang chủ");
            popup.getMenu().add("Tố cáo sản phẩm");
            popup.setOnMenuItemClickListener(item -> {
                if (item.getTitle().equals("Quay về trang chủ")) {
                    Intent intent = new Intent(this, ListActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(this, "Đã gửi báo cáo sản phẩm này", Toast.LENGTH_SHORT).show();
                }
                return true;
            });
            popup.show();
        });

        if ("admin".equals(userRole)) {
            binding.layoutCartShortcut.setVisibility(View.GONE);
        } else {
            binding.layoutCartShortcut.setVisibility(View.VISIBLE);
            binding.btnGoToCart.setOnClickListener(v -> startActivity(new Intent(this, CartActivity.class)));
            observeCartBadge();
        }
    }

    private void observeCartBadge() {
        if (userEmail == null || userEmail.isEmpty()) {
            binding.tvCartBadgeDetail.setVisibility(View.GONE);
            return;
        }
        cartViewModel.getCartItems(userEmail).observe(this, items -> {
            if (items != null) {
                int totalCount = 0;
                for (CartItem item : items) {
                    totalCount += item.getQuantity();
                }
                
                if (totalCount > 0) {
                    binding.tvCartBadgeDetail.setText(String.valueOf(totalCount));
                    binding.tvCartBadgeDetail.setVisibility(View.VISIBLE);
                } else {
                    binding.tvCartBadgeDetail.setVisibility(View.GONE);
                }
            } else {
                binding.tvCartBadgeDetail.setVisibility(View.GONE);
                Log.e("DetailActivity", "Failed to load cart items for badge");
            }
        });
    }

    private void shareProduct() {
        if (product == null) return;
        String shareBody = "Zendo Store - Sản phẩm: " + product.getName() + 
                "\nGiá chỉ: " + formatter.format(product.getPrice()) + "đ" +
                "\nXem ngay tại ứng dụng Zendo!";
        Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");
        sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Chia sẻ sản phẩm Zendo");
        sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareBody);
        startActivity(Intent.createChooser(sharingIntent, "Chia sẻ qua:"));
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadProductDetails();
        loadProductReviews(); 
    }

    private void loadSimilarProducts() {
        if (product == null || product.getCategory() == null) {
            binding.layoutSimilarProducts.setVisibility(View.GONE);
            return;
        }

        // Lấy pool sản phẩm cùng danh mục để Engine tính toán
        viewModel.getSimilarProducts(product.getCategory(), productId).observe(this, allInCategory -> {
            if (allInCategory != null && !allInCategory.isEmpty()) {
                // SỬ DỤNG ENGINE CÓ SẴN CỦA BẠN
                List<Product> recommended = RecommendationEngine.recommendSimilarProducts(product, allInCategory, 10);
                
                if (!recommended.isEmpty()) {
                    HorizontalProductAdapter adapter = new HorizontalProductAdapter(this, recommended);
                    binding.rvSimilarProducts.setAdapter(adapter);
                    binding.layoutSimilarProducts.setVisibility(View.VISIBLE);
                } else {
                    binding.layoutSimilarProducts.setVisibility(View.GONE);
                }
            } else {
                binding.layoutSimilarProducts.setVisibility(View.GONE);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void loadProductDetails() {
        if (productId == null) return;
        viewModel.getProductDetail(productId).observe(this, p -> {
            if (p != null) {
                product = p;
                displayData();
                checkIfLiked();
                loadSimilarProducts();
            }
        });
    }

    private void loadProductReviews() {
        if (productId == null) return;
        viewModel.getReviews(productId).observe(this, reviews -> {
            if (reviews == null) return;
            int count = reviews.size();
            binding.llTopReviewsContainer.removeAllViews();

            int s5 = 0, s4 = 0, s3 = 0, s2 = 0, s1 = 0;

            if (count > 0) {
                float totalRating = 0;
                for (Review review : reviews) {
                    float rating = review.getQualityRating();
                    totalRating += rating;

                    if (rating >= 5) s5++;
                    else if (rating >= 4) s4++;
                    else if (rating >= 3) s3++;
                    else if (rating >= 2) s2++;
                    else s1++;
                }

                binding.pb5Star.setMax(count); binding.pb5Star.setProgress(s5);
                binding.pb4Star.setMax(count); binding.pb4Star.setProgress(s4);
                binding.pb3Star.setMax(count); binding.pb3Star.setProgress(s3);
                binding.pb2Star.setMax(count); binding.pb2Star.setProgress(s2);
                binding.pb1Star.setMax(count); binding.pb1Star.setProgress(s1);

                reviews.sort((r1, r2) -> Float.compare(r2.getQualityRating(), r1.getQualityRating()));

                int topDisplayCount = Math.min(count, 3);
                for (int i = 0; i < topDisplayCount; i++) {
                    addReviewToLayout(reviews.get(i));
                }

                float average = totalRating / count;
                String ratingText = String.format(Locale.getDefault(), "%.1f", average);

                binding.tvBigRating.setText(ratingText);
                binding.tvReviewCountSubtitle.setText(count + " đánh giá");
                binding.tvTotalReviewsCount.setText(count + " đánh giá");
                binding.rbSmallSummary.setRating(average);
            }
        });
    }

    private void addReviewToLayout(Review review) {
        View reviewView = LayoutInflater.from(this).inflate(R.layout.item_review, binding.llTopReviewsContainer, false);
        
        com.google.android.material.imageview.ShapeableImageView ivAvatar = reviewView.findViewById(R.id.ivReviewUserAvatar);
        TextView tvName = reviewView.findViewById(R.id.tvReviewUserName);
        RatingBar rbStars = reviewView.findViewById(R.id.rbReviewStars);
        TextView tvDate = reviewView.findViewById(R.id.tvReviewDate);
        TextView tvComment = reviewView.findViewById(R.id.tvReviewComment);
        com.google.android.material.chip.ChipGroup cgTags = reviewView.findViewById(R.id.cgReviewTags);

        if (review.getUserEmail() != null) {
            new com.zendo.apps.data.repositories.UserRepository().getUser(review.getUserEmail()).observe(this, user -> {
                if (user != null) {
                    String displayName = user.getFullName();
                    if (review.isAnonymous() && displayName != null && displayName.length() > 2) {
                        displayName = displayName.charAt(0) + "***" + displayName.charAt(displayName.length() - 1);
                    }
                    tvName.setText(displayName != null ? displayName : "Người dùng Zendo");

                    String avatarData = user.getAvatar();
                    if (avatarData != null && !avatarData.isEmpty()) {
                        if (avatarData.startsWith("http")) {
                            Glide.with(this).load(avatarData).placeholder(R.drawable.ic_person).into(ivAvatar);
                        } else {
                            try {
                                byte[] decodedString = Base64.decode(avatarData, Base64.DEFAULT);
                                Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                                ivAvatar.setImageBitmap(decodedByte);
                            } catch (Exception e) {
                                ivAvatar.setImageResource(R.drawable.ic_person);
                            }
                        }
                    } else {
                        ivAvatar.setImageResource(R.drawable.ic_person);
                    }
                }
            });
        }

        rbStars.setRating(review.getQualityRating());
        
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            tvDate.setText(sdf.format(new Date(review.getTimestamp())));
        } catch (Exception e) {
            tvDate.setText("N/A");
        }
        
        tvComment.setText(review.getComment());
        cgTags.removeAllViews();
        if (review.getTags() != null) {
            for (String tag : review.getTags()) {
                Chip chip = new Chip(this);
                chip.setText(tag);
                chip.setChipMinHeight(20f);
                chip.setTextSize(10);
                cgTags.addView(chip);
            }
        }
        
        binding.llTopReviewsContainer.addView(reviewView);
    }

    private void displayData() {
        binding.tvProductDetailName.setText(product.getName());
        binding.tvProductDetailPrice.setText(formatPrice(product.getPrice()));
        binding.tvPriceBottom.setText(formatPrice(product.getPrice()));
        
        if (product.getOldPrice() > 0 && product.getOldPrice() > product.getPrice()) {
            binding.tvOldPrice.setText(formatter.format(product.getOldPrice()) + "đ");
            binding.tvOldPrice.setPaintFlags(binding.tvOldPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            binding.tvOldPrice.setVisibility(View.VISIBLE);
            
            int discount = (int) ((1 - (product.getPrice() / (float)product.getOldPrice())) * 100);
            if (discount > 0) {
                binding.tvDiscountTag.setText("-" + discount + "%");
                binding.tvDiscountTag.setVisibility(View.VISIBLE);
            } else {
                binding.tvDiscountTag.setVisibility(View.GONE);
            }
        } else {
            binding.tvOldPrice.setVisibility(View.GONE);
            binding.tvDiscountTag.setVisibility(View.GONE);
        }

        parseDescription(product.getDescription(), binding.tvProductDetailDesc);
        binding.tvProductDetailSold.setText("Đã bán " + (product.getSoldCount() > 0 ? product.getSoldCount() : "0"));
        
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat dayMonthFormat = new SimpleDateFormat("dd 'Th'MM", new Locale("vi", "VN"));
        cal.add(Calendar.DAY_OF_YEAR, 3);
        String dateStart = dayMonthFormat.format(cal.getTime());
        cal.add(Calendar.DAY_OF_YEAR, 2);
        String dateEnd = dayMonthFormat.format(cal.getTime());
        binding.tvShippingTime.setText("Nhận từ " + dateStart + " - " + dateEnd);

        List<String> imageList = new ArrayList<>();
        if (product.getImages() != null && !product.getImages().isEmpty()) {
            imageList.addAll(product.getImages());
        } else if (product.getImageUrl() != null) {
            imageList.add(product.getImageUrl());
        }

        ImageSliderAdapter adapter = new ImageSliderAdapter(imageList);
        binding.vpProductDetailImages.setAdapter(adapter);
        
        binding.tvImageIndicator.setText("1/" + imageList.size());
        binding.vpProductDetailImages.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                binding.tvImageIndicator.setText((position + 1) + "/" + imageList.size());
            }
        });

        binding.llImagePreviews.removeAllViews();
        float density = getResources().getDisplayMetrics().density;
        int sizePx = (int) (60 * density);
        int marginPx = (int) (8 * density);

        for (int i = 0; i < imageList.size(); i++) {
            ImageView iv = new ImageView(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(sizePx, sizePx);
            lp.setMargins(0, 0, marginPx, 0);
            iv.setLayoutParams(lp);
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            iv.setBackgroundResource(R.drawable.bg_border_gray_light);
            iv.setPadding(2, 2, 2, 2);
            
            String imgData = imageList.get(i);
            if (imgData != null) {
                if (imgData.startsWith("http")) {
                    Glide.with(this).load(imgData).into(iv);
                } else {
                    try {
                        byte[] decodedString = Base64.decode(imgData, Base64.DEFAULT);
                        Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                        iv.setImageBitmap(decodedByte);
                    } catch (Exception e) {
                        iv.setImageResource(R.drawable.logo_zendo);
                    }
                }
            }
            
            int finalI = i;
            iv.setOnClickListener(v -> binding.vpProductDetailImages.setCurrentItem(finalI, true));
            binding.llImagePreviews.addView(iv);
        }
    }

    private SpannableString formatPrice(double price) {
        String raw = formatter.format(price);
        String full = raw + "đ";
        SpannableString ss = new SpannableString(full);
        ss.setSpan(new RelativeSizeSpan(0.6f), raw.length(), full.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        ss.setSpan(new UnderlineSpan(), raw.length(), full.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return ss;
    }

    private void toggleCollapse(LinearLayout content, ImageView arrow) {
        if (content.getVisibility() == View.VISIBLE) {
            content.setVisibility(View.GONE);
            arrow.setImageResource(android.R.drawable.arrow_down_float);
        } else {
            content.setVisibility(View.VISIBLE);
            arrow.setImageResource(android.R.drawable.arrow_up_float);
        }
    }

    private void parseDescription(String desc, TextView tvGeneralDesc) {
        binding.tvSpecChip.setText("Đang cập nhật");
        binding.tvSpecScreen.setText("Đang cập nhật");
        binding.tvSpecRam.setText("Đang cập nhật");
        binding.tvSpecRom.setText("Đang cập nhật");
        binding.tvSpecPin.setText("Đang cập nhật");
        binding.tvSpecCamera.setText("Đang cập nhật");
        binding.tvSpecOs.setText("Đang cập nhật");
        
        String warranty = (product != null && product.getWarranty() != null) ? product.getWarranty() : "";
        if (!warranty.isEmpty()) {
            if (warranty.matches("\\d+")) warranty += " tháng";
            binding.tvSpecWarranty.setText(warranty);
            binding.tvProductDetailWarrantyMain.setText("Bảo hành: " + warranty);
        } else {
            binding.tvSpecWarranty.setText("Đang cập nhật");
            binding.tvProductDetailWarrantyMain.setText("Bảo hành: Đang cập nhật");
        }

        if (desc == null || desc.isEmpty()) return;
        
        String[] lines = desc.split("\\r?\\n");
        StringBuilder generalDesc = new StringBuilder();

        for (String line : lines) {
            String trimmedLine = line.trim();
            String lowerLine = trimmedLine.toLowerCase();

            if (lowerLine.startsWith("chip:")) {
                binding.tvSpecChip.setText(trimmedLine.substring(5).trim());
            } else if (lowerLine.startsWith("màn hình:")) {
                binding.tvSpecScreen.setText(trimmedLine.substring(9).trim());
            } else if (lowerLine.startsWith("ram:")) {
                binding.tvSpecRam.setText(trimmedLine.substring(4).trim());
            } else if (lowerLine.startsWith("bộ nhớ trong:")) {
                binding.tvSpecRom.setText(trimmedLine.substring(13).trim());
            } else if (lowerLine.startsWith("pin:")) {
                binding.tvSpecPin.setText(trimmedLine.substring(4).trim());
            } else if (lowerLine.startsWith("camera:")) {
                binding.tvSpecCamera.setText(trimmedLine.substring(7).trim());
            } else if (lowerLine.startsWith("hệ điều hành:")) {
                binding.tvSpecOs.setText(trimmedLine.substring(13).trim());
            } else if (!trimmedLine.isEmpty()) {
                generalDesc.append(trimmedLine).append("\n");
            }
        }
        tvGeneralDesc.setText(generalDesc.length() > 0 ? generalDesc.toString().trim() : "Không có mô tả.");
    }

    private void handleBuyNow() {
        if (product == null) return;
        ArrayList<CartItem> checkoutList = new ArrayList<>();
        CartItem tempItem = new CartItem(null, productId, product.getName(), 
                product.getPrice(), product.getImageUrl(), 1, userEmail);
        tempItem.setWarranty(product.getWarranty());
        checkoutList.add(tempItem);
        Intent intent = new Intent(this, CheckoutActivity.class);
        intent.putExtra("checkout_items", checkoutList);
        startActivity(intent);
    }

    private void addToCart() {
        if (userEmail == null || userEmail.isEmpty()) {
            Toast.makeText(this, "Vui lòng đăng nhập để thực hiện!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (product == null) return;

        CartItem item = new CartItem(null, productId, product.getName(),
                product.getPrice(), product.getImageUrl(), 1, userEmail);
        item.setWarranty(product.getWarranty());

        cartViewModel.addToCart(item).observe(this, state -> {
            if (state != null) {
                if (state.getStatus() == AuthResultState.Status.SUCCESS) {
                    Toast.makeText(DetailActivity.this, "Đã thêm vào giỏ hàng", Toast.LENGTH_SHORT).show();
                } else if (state.getStatus() == AuthResultState.Status.ERROR) {
                    Toast.makeText(DetailActivity.this, "Lỗi: " + state.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void showDeleteDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận xóa")
                .setMessage("Bạn có chắc chắn muốn xóa sản phẩm này không?")
                .setPositiveButton("Xóa", (dialog, which) -> deleteProduct())
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void deleteProduct() {
        if (productId == null) return;
        viewModel.deleteProduct(productId).observe(this, state -> {
            if (state.getStatus() == AuthResultState.Status.SUCCESS) {
                Toast.makeText(this, "Đã xóa sản phẩm thành công", Toast.LENGTH_SHORT).show();
                finish();
            } else if (state.getStatus() == AuthResultState.Status.ERROR) {
                Toast.makeText(this, "Lỗi: " + state.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}




