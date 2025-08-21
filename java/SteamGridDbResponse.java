package com.example.pegasusimagemanager;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class SteamGridDbResponse {
    @SerializedName("success")
    private boolean success;
    
    @SerializedName("data")
    private List<GameResult> data;
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public List<GameResult> getData() {
        return data;
    }
    
    public void setData(List<GameResult> data) {
        this.data = data;
    }
    
    public static class GameResult {
        @SerializedName("id")
        private int id;
        
        @SerializedName("name")
        private String name;
        
        @SerializedName("types")
        private List<String> types;
        
        @SerializedName("verified")
        private boolean verified;
        
        public int getId() {
            return id;
        }
        
        public void setId(int id) {
            this.id = id;
        }
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public List<String> getTypes() {
            return types;
        }
        
        public void setTypes(List<String> types) {
            this.types = types;
        }
        
        public boolean isVerified() {
            return verified;
        }
        
        public void setVerified(boolean verified) {
            this.verified = verified;
        }
    }
    
    // Classe para response de grids
    public static class GridResponse {
        @SerializedName("success")
        private boolean success;
        
        @SerializedName("data")
        private List<GridResult> data;
        
        public boolean isSuccess() {
            return success;
        }
        
        public void setSuccess(boolean success) {
            this.success = success;
        }
        
        public List<GridResult> getData() {
            return data;
        }
        
        public void setData(List<GridResult> data) {
            this.data = data;
        }
    }
    
    public static class GridResult {
        @SerializedName("id")
        private int id;
        
        @SerializedName("url")
        private String url;
        
        @SerializedName("thumb")
        private String thumb;

        @SerializedName("mime")
        private String mime;
        
        @SerializedName("tags")
        private List<String> tags;
        
        @SerializedName("author")
        private Author author;
        
        public int getId() {
            return id;
        }
        
        public void setId(int id) {
            this.id = id;
        }
        
        public String getUrl() {
            return url;
        }
        
        public void setUrl(String url) {
            this.url = url;
        }
        
        public String getThumb() {
            return thumb;
        }
        
        public void setThumb(String thumb) {
            this.thumb = thumb;
        }

        public String getMime() {
            return mime;
        }
        
        public List<String> getTags() {
            return tags;
        }
        
        public void setTags(List<String> tags) {
            this.tags = tags;
        }
        
        public Author getAuthor() {
            return author;
        }
        
        public void setAuthor(Author author) {
            this.author = author;
        }
        
        public static class Author {
            @SerializedName("name")
            private String name;
            
            @SerializedName("steam64")
            private String steam64;
            
            @SerializedName("avatar")
            private String avatar;
            
            public String getName() {
                return name;
            }
            
            public void setName(String name) {
                this.name = name;
            }
            
            public String getSteam64() {
                return steam64;
            }
            
            public void setSteam64(String steam64) {
                this.steam64 = steam64;
            }
            
            public String getAvatar() {
                return avatar;
            }
            
            public void setAvatar(String avatar) {
                this.avatar = avatar;
            }
        }
    }
}