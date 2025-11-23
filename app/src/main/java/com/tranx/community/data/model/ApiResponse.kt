package com.tranx.community.data.model

import com.google.gson.annotations.SerializedName

data class ApiResponse<T>(
    @SerializedName("code") val code: Int,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: T? = null
)

// 用户相关模型
data class User(
    @SerializedName("id") val id: Int,
    @SerializedName("username") val username: String,
    @SerializedName("email") val email: String? = null,
    @SerializedName("level") val level: Int = 0,
    @SerializedName("user_level") val userLevel: Int? = null,
    @SerializedName("exp") val exp: Int? = null,
    @SerializedName("coins") val coins: Int = 0,
    @SerializedName("avatar") val avatar: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null
)

data class LoginRequest(
    @SerializedName("username") val username: String,
    @SerializedName("password") val password: String
)

data class RegisterRequest(
    @SerializedName("username") val username: String,
    @SerializedName("password") val password: String,
    @SerializedName("email") val email: String? = null,
    @SerializedName("avatar") val avatar: String? = null
)

data class LoginResponse(
    @SerializedName("token") val token: String,
    @SerializedName("user") val user: User,
    @SerializedName("expires_at") val expiresAt: String
)

data class RegisterResponse(
    @SerializedName("user_id") val userId: Int,
    @SerializedName("username") val username: String
)

// 帖子相关模型
data class Post(
    @SerializedName("id") val id: Int,
    @SerializedName("board_id") val boardId: Int,
    @SerializedName("user_id") val userId: Int,
    @SerializedName("title") val title: String,
    @SerializedName("content") val content: String,
    @SerializedName("type") val type: String? = "text",
    @SerializedName("image_url") val imageUrl: String? = null,
    @SerializedName("publisher") val publisher: String,
    @SerializedName("publish_time") val publishTime: String,
    @SerializedName("coins") val coins: Int = 0,
    @SerializedName("favorites") val favorites: Int = 0,
    @SerializedName("likes") val likes: Int = 0,
    @SerializedName("image_url") val imageUrl: String? = null,
    @SerializedName("attachment_url") val attachmentUrl: String? = null,
    @SerializedName("attachment_type") val attachmentType: String? = null,
    @SerializedName("comment_count") val commentCount: Int = 0,
    @SerializedName("view_count") val viewCount: Int = 0,
    @SerializedName("last_reply_time") val lastReplyTime: String? = null,
    @SerializedName("is_liked") val isLiked: Boolean? = null,
    @SerializedName("is_favorited") val isFavorited: Boolean? = null
)

data class PostListResponse(
    @SerializedName("total") val total: Int,
    @SerializedName("page") val page: Int,
    @SerializedName("page_size") val pageSize: Int,
    @SerializedName("list") val list: List<Post>?
)

data class CreatePostRequest(
    @SerializedName("board_id") val boardId: Int,
    @SerializedName("title") val title: String,
    @SerializedName("content") val content: String,
    @SerializedName("type") val type: String = "text",
    @SerializedName("image_url") val imageUrl: String? = null
)

// 板块相关模型
data class Board(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("avatar_url") val avatarUrl: String? = null,
    @SerializedName("creator_id") val creatorId: Int? = null,
    @SerializedName("creator_name") val creatorName: String? = null,
    @SerializedName("creator_avatar") val creatorAvatar: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null,
    @SerializedName("created_at_ts") val createdAtTs: Long? = null,
    @SerializedName("updated_at_ts") val updatedAtTs: Long? = null
)

// 评论相关模型
data class Comment(
    @SerializedName("id") val id: Int,
    @SerializedName("post_id") val postId: Int,
    @SerializedName("user_id") val userId: Int,
    @SerializedName("parent_id") val parentId: Int? = null,
    @SerializedName("username") val username: String? = null,
    @SerializedName("avatar") val avatar: String? = null,
    @SerializedName("content") val content: String? = null,
    @SerializedName("publish_time") val publishTime: String? = null,
    @SerializedName("floor") val floor: Int? = null,
    @SerializedName("is_author") val isAuthor: Boolean = false,
    @SerializedName("is_liked") val isLiked: Boolean? = null,
    @SerializedName("likes") val likes: Int? = null,
    @SerializedName("coins") val coins: Int? = null,
    @SerializedName("reply_count") val replyCount: Int? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null
)

data class CommentListResponse(
    @SerializedName("total") val total: Int,
    @SerializedName("page") val page: Int,
    @SerializedName("page_size") val pageSize: Int,
    @SerializedName("list") val list: List<Comment>
)

data class CreateCommentRequest(
    @SerializedName("post_id") val postId: Int,
    @SerializedName("parent_id") val parentId: Int? = null,
    @SerializedName("content") val content: String
)

// 收藏夹相关模型
data class Folder(
    @SerializedName("id") val id: Int,
    @SerializedName("user_id") val userId: Int,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("is_public") val isPublic: Boolean = true,
    @SerializedName("item_count") val itemCount: Int = 0,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null
)

data class CreateFolderRequest(
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("is_public") val isPublic: Boolean = true
)

data class CreateFolderResponse(
    @SerializedName("folder_id") val folderId: Int
)

data class CreateBoardRequest(
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("avatar_url") val avatarUrl: String? = null
)

// 签到相关模型
data class CheckinResponse(
    @SerializedName("reward") val reward: Int? = null,
    @SerializedName("reward_coins") val rewardCoins: Int? = null,
    @SerializedName("reward_exp") val rewardExp: Int? = null,
    @SerializedName("total_coins") val totalCoins: Int? = null,
    @SerializedName("total_exp") val totalExp: Int? = null,
    @SerializedName("user_level") val userLevel: Int? = null,
    @SerializedName("check_time") val checkTime: String
)

data class CheckinStatus(
    @SerializedName("checked_in") val checkedIn: Boolean,
    @SerializedName("can_check") val canCheck: Boolean,
    @SerializedName("check_time") val checkTime: String? = null,
    @SerializedName("reward") val reward: Int? = null
)

// 用户统计
data class UserStats(
    @SerializedName("following_count") val followingCount: Int,
    @SerializedName("follower_count") val followerCount: Int,
    @SerializedName("is_following") val isFollowing: Boolean = false
)

data class UserListResponse(
    @SerializedName("total") val total: Int,
    @SerializedName("page") val page: Int,
    @SerializedName("page_size") val pageSize: Int,
    @SerializedName("list") val list: List<User>
)

// 应用市场相关模型
data class App(
    @SerializedName("package_name") val packageName: String,
    @SerializedName("name") val name: String,
    @SerializedName("icon_url") val iconUrl: String? = null,
    @SerializedName("version") val version: String,
    @SerializedName("size") val size: Long = 0,
    @SerializedName("rating") val rating: Double = 0.0
)

data class AppDetail(
    @SerializedName("package_name") val packageName: String,
    @SerializedName("name") val name: String,
    @SerializedName("icon_url") val iconUrl: String? = null,
    @SerializedName("version") val version: String,
    @SerializedName("version_code") val versionCode: Int = 0,
    @SerializedName("size") val size: Long = 0,
    @SerializedName("rating") val rating: Double = 0.0,
    @SerializedName("rating_count") val ratingCount: Int = 0,
    @SerializedName("description") val description: String? = null,
    @SerializedName("screenshots") val screenshots: List<String>? = null,
    @SerializedName("tags") val tags: List<String>? = null,
    @SerializedName("download_url") val downloadUrl: String? = null,
    @SerializedName("total_coins") val totalCoins: Int = 0,
    @SerializedName("download_count") val downloadCount: Int = 0,
    @SerializedName("uploader_name") val uploaderName: String? = null,
    @SerializedName("update_content") val updateContent: String? = null,
    @SerializedName("update_time") val updateTime: String? = null,
    @SerializedName("main_category") val mainCategory: String? = null,
    @SerializedName("sub_category") val subCategory: String? = null,
    @SerializedName("channel") val channel: String? = null,
    @SerializedName("share_desc") val shareDesc: String? = null,
    @SerializedName("developer_name") val developerName: String? = null,
    @SerializedName("ad_level") val adLevel: String? = null,
    @SerializedName("payment_type") val paymentType: String? = null,
    @SerializedName("operation_type") val operationType: String? = null
)

data class AppListResponse(
    @SerializedName("total") val total: Int,
    @SerializedName("page") val page: Int,
    @SerializedName("page_size") val pageSize: Int,
    @SerializedName("list") val list: List<App>?
)

data class CategoryResponse(
    @SerializedName("main_category") val mainCategory: String,
    @SerializedName("sub_categories") val subCategories: List<String>?
)

data class UploadAppRequest(
    @SerializedName("package_name") val packageName: String,
    @SerializedName("name") val name: String,
    @SerializedName("icon_url") val iconUrl: String? = null,
    @SerializedName("version") val version: String,
    @SerializedName("version_code") val versionCode: Int,
    @SerializedName("size") val size: Long,
    @SerializedName("channel") val channel: String,
    @SerializedName("main_category") val mainCategory: String,
    @SerializedName("sub_category") val subCategory: String,
    @SerializedName("screenshots") val screenshots: List<String>? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("share_desc") val shareDesc: String? = null,
    @SerializedName("update_content") val updateContent: String? = null,
    @SerializedName("developer_name") val developerName: String? = null,
    @SerializedName("ad_level") val adLevel: String = "none",
    @SerializedName("payment_type") val paymentType: String = "free",
    @SerializedName("operation_type") val operationType: String = "indie",
    @SerializedName("download_url") val downloadUrl: String
)

data class UploadTask(
    @SerializedName("task_id") val taskId: Int,
    @SerializedName("package_name") val packageName: String,
    @SerializedName("name") val name: String,
    @SerializedName("icon_url") val iconUrl: String? = null,
    @SerializedName("version") val version: String,
    @SerializedName("status") val status: String,
    @SerializedName("status_label") val statusLabel: String? = null,
    @SerializedName("upload_time") val uploadTime: String? = null
)

data class UploadTaskListResponse(
    @SerializedName("total") val total: Int,
    @SerializedName("page") val page: Int,
    @SerializedName("page_size") val pageSize: Int,
    @SerializedName("list") val list: List<UploadTask>?
)

data class UploadTaskResponse(
    @SerializedName("task_id") val taskId: Int,
    @SerializedName("status") val status: String,
    @SerializedName("uploader") val uploader: String? = null,
    @SerializedName("upload_time") val uploadTime: String? = null
)

data class CoinRequest(
    @SerializedName("coins") val coins: Int
)

data class CoinResponse(
    @SerializedName("total_coins") val totalCoins: Int
)

data class LikeResult(
    @SerializedName("likes") val likes: Int,
    @SerializedName("is_liked") val isLiked: Boolean
)

data class CoinResult(
    @SerializedName("coins") val coins: Int,
    @SerializedName("user_coins") val userCoins: Int? = null
)

// Picui 图床相关
data class PicuiUploadResponse(
    @SerializedName("status") val status: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: PicuiUploadData?
)

data class PicuiUploadData(
    @SerializedName("key") val key: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("links") val links: PicuiLinks?
)

data class PicuiLinks(
    @SerializedName("url") val url: String?,
    @SerializedName("html") val html: String?,
    @SerializedName("bbcode") val bbcode: String?,
    @SerializedName("markdown") val markdown: String?,
    @SerializedName("markdown_with_link") val markdownWithLink: String?,
    @SerializedName("thumbnail_url") val thumbnailUrl: String?,
    @SerializedName("delete_url") val deleteUrl: String?
)

