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
    @SerializedName("last_reply_time") val lastReplyTime: String? = null
)

data class PostListResponse(
    @SerializedName("total") val total: Int,
    @SerializedName("page") val page: Int,
    @SerializedName("page_size") val pageSize: Int,
    @SerializedName("list") val list: List<Post>
)

data class CreatePostRequest(
    @SerializedName("board_id") val boardId: Int,
    @SerializedName("title") val title: String,
    @SerializedName("content") val content: String,
    @SerializedName("image_url") val imageUrl: String? = null
)

// 板块相关模型
data class Board(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null
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
    @SerializedName("description") val description: String? = null
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

