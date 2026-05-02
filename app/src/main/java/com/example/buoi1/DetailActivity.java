package com.example.buoi1;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DetailActivity extends AppCompatActivity {

    private DecimalFormat formatter = new DecimalFormat("###,###,###");
    private FirebaseFirestore db;
    private String productId;
    private Product product;
    private String userEmail, userRole;

    private TextView tvSpecChip, tvSpecScreen, tvSpecRam, tvSpecRom, tvSpecPin, tvSpecCamera, tvSpecOs, tvSpecWarranty;
    private ImageView ivArrowSpecs, ivArrowDesc;
    private TextView tvPrice, tvOldPrice, tvDiscountTag, tvName, tvDesc, tvRating, tvSold, tvStock, tvShippingTime, tvPriceBottom;
    private LinearLayout layoutContentSpecs, layoutContentDesc;
    
    private TextView tvBigRating, tvReviewCountSubtitle, tvTotalReviewsCount, tvViewAllReviews, tvImageIndicator, tvCartBadge;
    private RatingBar rbSmallSummary;
    private ViewPager2 vpImages;
    private LinearLayout llTopReviewsContainer, llImagePreviews;
    private ProgressBar pb5, pb4, pb3, pb2, pb1;
    private ImageView ivFavorite;
    private ListenerRegistration cartListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Làm Status Bar trong suốt và icon tối màu
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.TRANSPARENT);
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }

        setContentView(R.layout.activity_detail);

        db = FirebaseFirestore.getInstance();
        SharedPreferences sharedPref = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        userEmail = sharedPref.getString("user_email", "");
        userRole = sharedPref.getString("user_role", "user");

        initViews();
        setupHeaderActions();
        
        product = (Product) getIntent().getSerializableExtra("product_data");
        if (product != null) {
            productId = product.getId();
            displayData();
            checkIfLiked();
        }

        if (tvViewAllReviews != null) {
            tvViewAllReviews.setOnClickListener(v -> {
                Intent intent = new Intent(this, ReviewListActivity.class);
                intent.putExtra("product_id", productId);
                startActivity(intent);
            });
        }
    }

    private void initViews() {
        vpImages = findViewById(R.id.vpProductDetailImages);
        tvImageIndicator = findViewById(R.id.tvImageIndicator);
        tvCartBadge = findViewById(R.id.tvCartBadgeDetail);
        llImagePreviews = findViewById(R.id.llImagePreviews);
        
        tvPrice = findViewById(R.id.tvProductDetailPrice);
        tvOldPrice = findViewById(R.id.tvOldPrice);
        tvDiscountTag = findViewById(R.id.tvDiscountTag);
        tvName = findViewById(R.id.tvProductDetailName);
        tvDesc = findViewById(R.id.tvProductDetailDesc);
        tvSold = findViewById(R.id.tvProductDetailSold);
        ivFavorite = findViewById(R.id.ivFavorite);
        tvShippingTime = findViewById(R.id.tvShippingTime);
        tvPriceBottom = findViewById(R.id.tvPriceBottom);
        
        tvBigRating = findViewById(R.id.tvBigRating);
        tvReviewCountSubtitle = findViewById(R.id.tvReviewCountSubtitle);
        tvTotalReviewsCount = findViewById(R.id.tvTotalReviewsCount);
        rbSmallSummary = findViewById(R.id.rbSmallSummary);
        tvViewAllReviews = findViewById(R.id.tvViewAllReviews);
        llTopReviewsContainer = findViewById(R.id.llTopReviewsContainer);
        
        pb5 = findViewById(R.id.pb5Star);
        pb4 = findViewById(R.id.pb4Star);
        pb3 = findViewById(R.id.pb3Star);
        pb2 = findViewById(R.id.pb2Star);
        pb1 = findViewById(R.id.pb1Star);
        
        tvSpecChip = findViewById(R.id.tvSpecChip);
        tvSpecScreen = findViewById(R.id.tvSpecScreen);
        tvSpecRam = findViewById(R.id.tvSpecRam);
        tvSpecRom = findViewById(R.id.tvSpecRom);
        tvSpecPin = findViewById(R.id.tvSpecPin);
        tvSpecCamera = findViewById(R.id.tvSpecCamera);
        tvSpecOs = findViewById(R.id.tvSpecOs);
        tvSpecWarranty = findViewById(R.id.tvSpecWarranty);

        layoutContentSpecs = findViewById(R.id.layoutContentSpecs);
        layoutContentDesc = findViewById(R.id.layoutContentDesc);
        ivArrowSpecs = findViewById(R.id.ivArrowSpecs);
        ivArrowDesc = findViewById(R.id.ivArrowDesc);
        LinearLayout layoutHeaderSpecs = findViewById(R.id.layoutHeaderSpecs);
        LinearLayout layoutHeaderDesc = findViewById(R.id.layoutHeaderDesc);
        
        if (layoutHeaderSpecs != null) {
            layoutHeaderSpecs.setOnClickListener(v -> toggleCollapse(layoutContentSpecs, ivArrowSpecs));
        }
        if (layoutHeaderDesc != null) {
            layoutHeaderDesc.setOnClickListener(v -> toggleCollapse(layoutContentDesc, ivArrowDesc));
        }

        LinearLayout layoutUser = findViewById(R.id.layoutUserActions);
        LinearLayout layoutAdmin = findViewById(R.id.layoutAdminActions);
        
        if ("admin".equals(userRole)) {
            if (layoutAdmin != null) layoutAdmin.setVisibility(View.VISIBLE);
            if (layoutUser != null) layoutUser.setVisibility(View.GONE);
        } else {
            if (layoutAdmin != null) layoutAdmin.setVisibility(View.GONE);
            if (layoutUser != null) layoutUser.setVisibility(View.VISIBLE);
        }

        Button btnEdit = findViewById(R.id.btnEditProduct);
        Button btnDelete = findViewById(R.id.btnDeleteProduct);
        if (btnEdit != null) {
            btnEdit.setOnClickListener(v -> {
                Intent intent = new Intent(this, AddProductActivity.class);
                intent.putExtra("edit_product", product);
                startActivity(intent);
            });
        }
        if (btnDelete != null) {
            btnDelete.setOnClickListener(v -> showDeleteDialog());
        }

        View btnBuyNow = findViewById(R.id.btnBuyNow);
        View btnAddToCart = findViewById(R.id.btnAddToCart);
        View btnChatNow = findViewById(R.id.btnChatNowDetail);

        if (btnBuyNow != null) btnBuyNow.setOnClickListener(v -> handleBuyNow());
        if (btnAddToCart != null) btnAddToCart.setOnClickListener(v -> addToCart());
        if (btnChatNow != null) {
            btnChatNow.setOnClickListener(v -> {
                Intent intent = new Intent(this, ChatActivity.class);
                startActivity(intent);
            });
        }

        if (ivFavorite != null) {
            ivFavorite.setOnClickListener(v -> toggleFavorite());
        }

        View btnViewWarrantyPolicy = findViewById(R.id.btnViewWarrantyPolicy);
        if (btnViewWarrantyPolicy != null) {
            btnViewWarrantyPolicy.setOnClickListener(v -> {
                startActivity(new Intent(this, WarrantyPolicyActivity.class));
            });
        }
    }

    private void checkIfLiked() {
        if (product != null && userEmail != null && ivFavorite != null) {
            if (product.getLikedBy() != null && product.getLikedBy().contains(userEmail)) {
                ivFavorite.setImageResource(R.drawable.ic_heart_filled);
                ivFavorite.setColorFilter(getResources().getColor(R.color.pink_heart));
            } else {
                ivFavorite.setImageResource(R.drawable.ic_heart_outline);
                ivFavorite.setColorFilter(getResources().getColor(R.color.gray_inactive));
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
            db.collection("products").document(productId)
                    .update("likedBy", FieldValue.arrayRemove(userEmail))
                    .addOnSuccessListener(aVoid -> {
                        product.getLikedBy().remove(userEmail);
                        checkIfLiked();
                        Toast.makeText(this, "Đã bỏ yêu thích", Toast.LENGTH_SHORT).show();
                    });
        } else {
            db.collection("products").document(productId)
                    .update("likedBy", FieldValue.arrayUnion(userEmail))
                    .addOnSuccessListener(aVoid -> {
                        if (product.getLikedBy() == null) product.setLikedBy(new ArrayList<>());
                        product.getLikedBy().add(userEmail);
                        checkIfLiked();
                        Toast.makeText(this, "Đã thêm vào yêu thích", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void setupHeaderActions() {
        ImageButton btnBack = findViewById(R.id.btnBackDetail);
        ImageButton btnShare = findViewById(R.id.btnShareProduct);
        ImageButton btnGoToCart = findViewById(R.id.btnGoToCart);
        ImageButton btnMore = findViewById(R.id.btnMoreDetail);
        View layoutCart = findViewById(R.id.layoutCartShortcut);

        if (btnBack != null) btnBack.setOnClickListener(v -> finish());
        if (btnShare != null) btnShare.setOnClickListener(v -> shareProduct());
        
        if (btnMore != null) {
            btnMore.setOnClickListener(v -> {
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
        }

        if ("admin".equals(userRole)) {
            if (layoutCart != null) layoutCart.setVisibility(View.GONE);
        } else {
            if (layoutCart != null) {
                layoutCart.setVisibility(View.VISIBLE);
                btnGoToCart.setOnClickListener(v -> startActivity(new Intent(this, CartActivity.class)));
                observeCartBadge();
            }
        }
    }

    private void observeCartBadge() {
        if (userEmail == null || userEmail.isEmpty()) return;
        cartListener = db.collection("cart")
                .whereEqualTo("userEmail", userEmail)
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;
                    int count = 0;
                    for (QueryDocumentSnapshot doc : value) {
                        Long qty = doc.getLong("quantity");
                        if (qty != null) count += qty.intValue();
                    }
                    if (count > 0) {
                        tvCartBadge.setText(String.valueOf(count));
                        tvCartBadge.setVisibility(View.VISIBLE);
                    } else {
                        tvCartBadge.setVisibility(View.GONE);
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cartListener != null) cartListener.remove();
    }

    private void loadProductDetails() {
        if (productId == null) return;
        db.collection("products").document(productId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    product = documentSnapshot.toObject(Product.class);
                    if (product != null) {
                        displayData();
                        checkIfLiked();
                    }
                });
    }

    private void loadProductReviews() {
        if (productId == null) return;
        db.collection("reviews")
                .whereEqualTo("productId", productId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int count = queryDocumentSnapshots.size();
                    if (llTopReviewsContainer != null) llTopReviewsContainer.removeAllViews();
                    
                    int s5 = 0, s4 = 0, s3 = 0, s2 = 0, s1 = 0;
                    
                    if (count > 0) {
                        float totalRating = 0;
                        List<Review> allReviews = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            Review review = doc.toObject(Review.class);
                            float rating = review.getQualityRating();
                            totalRating += rating;
                            allReviews.add(review);
                            
                            if (rating >= 5) s5++;
                            else if (rating >= 4) s4++;
                            else if (rating >= 3) s3++;
                            else if (rating >= 2) s2++;
                            else s1++;
                        }
                        
                        if (pb5 != null) { pb5.setMax(count); pb5.setProgress(s5); }
                        if (pb4 != null) { pb4.setMax(count); pb4.setProgress(s4); }
                        if (pb3 != null) { pb3.setMax(count); pb3.setProgress(s3); }
                        if (pb2 != null) { pb2.setMax(count); pb2.setProgress(s2); }
                        if (pb1 != null) { pb1.setMax(count); pb1.setProgress(s1); }
                        
                        allReviews.sort((r1, r2) -> Float.compare(r2.getQualityRating(), r1.getQualityRating()));
                        
                        int topDisplayCount = Math.min(count, 3);
                        for (int i = 0; i < topDisplayCount; i++) {
                            addReviewToLayout(allReviews.get(i));
                        }

                        float average = totalRating / count;
                        String ratingText = String.format(Locale.getDefault(), "%.1f", average);
                        
                        if (tvBigRating != null) tvBigRating.setText(ratingText);
                        if (tvReviewCountSubtitle != null) tvReviewCountSubtitle.setText(count + " đánh giá");
                        if (tvTotalReviewsCount != null) tvTotalReviewsCount.setText(count + " đánh giá");
                        if (rbSmallSummary != null) rbSmallSummary.setRating(average);
                    }
                });
    }

    private void addReviewToLayout(Review review) {
        if (llTopReviewsContainer == null) return;
        View reviewView = LayoutInflater.from(this).inflate(R.layout.item_review, llTopReviewsContainer, false);
        
        ShapeableImageView ivAvatar = reviewView.findViewById(R.id.ivReviewUserAvatar);
        TextView tvName = reviewView.findViewById(R.id.tvReviewUserName);
        RatingBar rbStars = reviewView.findViewById(R.id.rbReviewStars);
        TextView tvDate = reviewView.findViewById(R.id.tvReviewDate);
        TextView tvComment = reviewView.findViewById(R.id.tvReviewComment);
        ChipGroup cgTags = reviewView.findViewById(R.id.cgReviewTags);

        if (review.getUserEmail() != null) {
            db.collection("users").whereEqualTo("email", review.getUserEmail()).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        User user = queryDocumentSnapshots.getDocuments().get(0).toObject(User.class);
                        if (user != null) {
                            String displayName = user.getFullName();
                            if (review.isAnonymous() && displayName != null && displayName.length() > 2) {
                                displayName = displayName.charAt(0) + "***" + displayName.charAt(displayName.length() - 1);
                            }
                            if (tvName != null) tvName.setText(displayName != null ? displayName : "Người dùng Zendo");

                            String avatarData = user.getAvatar();
                            if (avatarData != null && !avatarData.isEmpty()) {
                                if (avatarData.startsWith("http")) {
                                    Glide.with(this).load(avatarData).placeholder(R.drawable.ic_person).into(ivAvatar);
                                } else {
                                    try {
                                        byte[] decodedString = Base64.decode(avatarData, Base64.DEFAULT);
                                        Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                                        if (ivAvatar != null) ivAvatar.setImageBitmap(decodedByte);
                                    } catch (Exception e) {
                                        if (ivAvatar != null) ivAvatar.setImageResource(R.drawable.ic_person);
                                    }
                                }
                            } else {
                                if (ivAvatar != null) ivAvatar.setImageResource(R.drawable.ic_person);
                            }
                        }
                    }
                });
        }

        if (rbStars != null) rbStars.setRating(review.getQualityRating());
        
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            if (tvDate != null) tvDate.setText(sdf.format(new Date(review.getTimestamp())));
        } catch (Exception e) {
            if (tvDate != null) tvDate.setText("N/A");
        }
        
        if (tvComment != null) tvComment.setText(review.getComment());
        if (cgTags != null) {
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
        }
        
        llTopReviewsContainer.addView(reviewView);
    }

    private void displayData() {
        if (tvName != null) tvName.setText(product.getName());
        
        if (tvPrice != null) tvPrice.setText(formatPrice(product.getPrice()));
        if (tvPriceBottom != null) tvPriceBottom.setText(formatPrice(product.getPrice()));
        
        if (product.getOldPrice() > 0 && product.getOldPrice() > product.getPrice()) {
            if (tvOldPrice != null) {
                tvOldPrice.setText(formatter.format(product.getOldPrice()) + "đ");
                tvOldPrice.setPaintFlags(tvOldPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                tvOldPrice.setVisibility(View.VISIBLE);
            }
            
            int discount = (int) ((1 - (product.getPrice() / (float)product.getOldPrice())) * 100);
            if (discount > 0) {
                if (tvDiscountTag != null) {
                    tvDiscountTag.setText("-" + discount + "%");
                    tvDiscountTag.setVisibility(View.VISIBLE);
                }
            } else {
                if (tvDiscountTag != null) tvDiscountTag.setVisibility(View.GONE);
            }
        } else {
            if (tvOldPrice != null) tvOldPrice.setVisibility(View.GONE);
            if (tvDiscountTag != null) tvDiscountTag.setVisibility(View.GONE);
        }

        if (tvDesc != null) parseDescription(product.getDescription(), tvDesc);
        if (tvSold != null) tvSold.setText("Đã bán " + (product.getSoldCount() > 0 ? product.getSoldCount() : "0"));
        
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat dayMonthFormat = new SimpleDateFormat("dd 'Th'MM", new Locale("vi", "VN"));
        cal.add(Calendar.DAY_OF_YEAR, 3);
        String dateStart = dayMonthFormat.format(cal.getTime());
        cal.add(Calendar.DAY_OF_YEAR, 2);
        String dateEnd = dayMonthFormat.format(cal.getTime());
        if (tvShippingTime != null) {
            tvShippingTime.setText("Nhận từ " + dateStart + " - " + dateEnd);
        }

        List<String> imageList = new ArrayList<>();
        if (product.getImages() != null && !product.getImages().isEmpty()) {
            imageList.addAll(product.getImages());
        } else if (product.getImageUrl() != null) {
            imageList.add(product.getImageUrl());
        }

        if (vpImages != null) {
            ImageSliderAdapter adapter = new ImageSliderAdapter(imageList);
            vpImages.setAdapter(adapter);
            
            if (tvImageIndicator != null) tvImageIndicator.setText("1/" + imageList.size());
            vpImages.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                @Override
                public void onPageSelected(int position) {
                    super.onPageSelected(position);
                    if (tvImageIndicator != null) tvImageIndicator.setText((position + 1) + "/" + imageList.size());
                }
            });
        }

        if (llImagePreviews != null) {
            llImagePreviews.removeAllViews();
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
                            if (iv != null) iv.setImageBitmap(decodedByte);
                        } catch (Exception e) {
                            if (iv != null) iv.setImageResource(R.drawable.logo_zendo);
                        }
                    }
                }
                
                int finalI = i;
                iv.setOnClickListener(v -> vpImages.setCurrentItem(finalI, true));
                llImagePreviews.addView(iv);
            }
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
        if (content != null) {
            if (content.getVisibility() == View.VISIBLE) {
                content.setVisibility(View.GONE);
                if (arrow != null) arrow.setImageResource(android.R.drawable.arrow_down_float);
            } else {
                content.setVisibility(View.VISIBLE);
                if (arrow != null) arrow.setImageResource(android.R.drawable.arrow_up_float);
            }
        }
    }

    private void parseDescription(String desc, TextView tvGeneralDesc) {
        if (desc == null || desc.isEmpty()) return;
        
        if (tvSpecChip != null) tvSpecChip.setText("Đang cập nhật");
        if (tvSpecScreen != null) tvSpecScreen.setText("Đang cập nhật");
        if (tvSpecRam != null) tvSpecRam.setText("Đang cập nhật");
        if (tvSpecRom != null) tvSpecRom.setText("Đang cập nhật");
        if (tvSpecPin != null) tvSpecPin.setText("Đang cập nhật");
        if (tvSpecCamera != null) tvSpecCamera.setText("Đang cập nhật");
        if (tvSpecOs != null) tvSpecOs.setText("Đang cập nhật");
        if (tvSpecWarranty != null) {
            String warranty = (product != null && product.getWarranty() != null) ? product.getWarranty() : "";
            if (!warranty.isEmpty()) {
                if (warranty.matches("\\d+")) warranty += " tháng";
                tvSpecWarranty.setText(warranty);
            } else {
                tvSpecWarranty.setText("Đang cập nhật");
            }
        }

        String[] lines = desc.split("\\r?\\n");
        StringBuilder generalDesc = new StringBuilder();

        for (String line : lines) {
            String trimmedLine = line.trim();
            String lowerLine = trimmedLine.toLowerCase();

            if (lowerLine.startsWith("chip:")) {
                if (tvSpecChip != null) tvSpecChip.setText(trimmedLine.substring(5).trim());
            } else if (lowerLine.startsWith("màn hình:")) {
                if (tvSpecScreen != null) tvSpecScreen.setText(trimmedLine.substring(9).trim());
            } else if (lowerLine.startsWith("ram:")) {
                if (tvSpecRam != null) tvSpecRam.setText(trimmedLine.substring(4).trim());
            } else if (lowerLine.startsWith("bộ nhớ trong:")) {
                if (tvSpecRom != null) tvSpecRom.setText(trimmedLine.substring(13).trim());
            } else if (lowerLine.startsWith("pin:")) {
                if (tvSpecPin != null) tvSpecPin.setText(trimmedLine.substring(4).trim());
            } else if (lowerLine.startsWith("camera:")) {
                if (tvSpecCamera != null) tvSpecCamera.setText(trimmedLine.substring(7).trim());
            } else if (lowerLine.startsWith("hệ điều hành:")) {
                if (tvSpecOs != null) tvSpecOs.setText(trimmedLine.substring(13).trim());
            } else if (!trimmedLine.isEmpty()) {
                generalDesc.append(trimmedLine).append("\n");
            }
        }
        if (tvGeneralDesc != null) tvGeneralDesc.setText(generalDesc.length() > 0 ? generalDesc.toString().trim() : "Không có mô tả.");
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
        if (product == null || userEmail.isEmpty()) return;
        db.collection("cart")
                .whereEqualTo("userEmail", userEmail)
                .whereEqualTo("productId", productId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        String docId = queryDocumentSnapshots.getDocuments().get(0).getId();
                        db.collection("cart").document(docId).update("quantity", FieldValue.increment(1));
                    } else {
                        CartItem newItem = new CartItem(null, productId, product.getName(), 
                                product.getPrice(), product.getImageUrl(), 1, userEmail);
                        newItem.setWarranty(product.getWarranty());
                        db.collection("cart").add(newItem);
                    }
                    Toast.makeText(this, "Đã thêm vào giỏ hàng!", Toast.LENGTH_SHORT).show();
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
        db.collection("products").document(productId).delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Đã xóa sản phẩm", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
