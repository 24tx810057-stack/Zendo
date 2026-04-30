package com.example.buoi1;

import java.util.List;

public class AddressModels {
    public static class Province {
        private String name;
        private int code;
        private List<District> districts;

        public String getName() { return name; }
        public int getCode() { return code; }
        public List<District> getDistricts() { return districts; }
        @Override
        public String toString() { return name; }
    }

    public static class District {
        private String name;
        private int code;
        private List<Ward> wards;

        public String getName() { return name; }
        public int getCode() { return code; }
        public List<Ward> getWards() { return wards; }
        @Override
        public String toString() { return name; }
    }

    public static class Ward {
        private String name;
        private int code;

        public String getName() { return name; }
        public int getCode() { return code; }
        @Override
        public String toString() { return name; }
    }
}
