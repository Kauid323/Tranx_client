package com.tranx.community.data.api

import com.tranx.community.data.model.*
import retrofit2.http.*

interface ApiService {
    // 认证相关
    @POST("/api/auth/register")
    suspend fun register(@Body request: RegisterRequest): ApiResponse<RegisterResponse>

    @POST("/api/auth/login")
    suspend fun login(@Body request: LoginRequest): ApiResponse<LoginResponse>

    @POST("/api/logout")
    suspend fun logout(@Header("Token") token: String): ApiResponse<Unit>

    // 用户相关
    @GET("/api/me")
    suspend fun getCurrentUser(@Header("Token") token: String): ApiResponse<Map<String, Any>>

    @GET("/api/users/{id}")
    suspend fun getUser(
        @Header("Token") token: String,
        @Path("id") userId: Int
    ): ApiResponse<Map<String, Any>>

    @GET("/api/users")
    suspend fun getUserList(
        @Header("Token") token: String,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20
    ): ApiResponse<UserListResponse>

    @GET("/api/users/{id}/stats")
    suspend fun getUserStats(
        @Header("Token") token: String,
        @Path("id") userId: Int
    ): ApiResponse<UserStats>

    // 关注相关
    @POST("/api/follow/{id}")
    suspend fun followUser(
        @Header("Token") token: String,
        @Path("id") userId: Int
    ): ApiResponse<Unit>

    @DELETE("/api/follow/{id}")
    suspend fun unfollowUser(
        @Header("Token") token: String,
        @Path("id") userId: Int
    ): ApiResponse<Unit>

    @GET("/api/follow/{id}/following")
    suspend fun getFollowingList(
        @Header("Token") token: String,
        @Path("id") userId: Int,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20
    ): ApiResponse<UserListResponse>

    @GET("/api/follow/{id}/followers")
    suspend fun getFollowerList(
        @Header("Token") token: String,
        @Path("id") userId: Int,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20
    ): ApiResponse<UserListResponse>

    // 签到相关
    @POST("/api/checkin")
    suspend fun checkin(@Header("Token") token: String): ApiResponse<CheckinResponse>

    @GET("/api/checkin/status")
    suspend fun getCheckinStatus(@Header("Token") token: String): ApiResponse<CheckinStatus>

    // 板块相关
    @GET("/api/boards/list")
    suspend fun getBoardList(@Header("Token") token: String): ApiResponse<List<Board>>

    @GET("/api/boards/{id}")
    suspend fun getBoard(
        @Header("Token") token: String,
        @Path("id") boardId: Int
    ): ApiResponse<Board>

    // 帖子相关
    @POST("/api/posts/create")
    suspend fun createPost(
        @Header("Token") token: String,
        @Body request: CreatePostRequest
    ): ApiResponse<Map<String, Any>>

    @GET("/api/posts/list")
    suspend fun getPostList(
        @Header("Token") token: String,
        @Query("board_id") boardId: Int? = null,
        @Query("sort") sort: String = "latest",
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20
    ): ApiResponse<PostListResponse>

    @GET("/api/posts/{id}")
    suspend fun getPost(
        @Header("Token") token: String,
        @Path("id") postId: Int
    ): ApiResponse<Post>

    @PUT("/api/posts/{id}")
    suspend fun updatePost(
        @Header("Token") token: String,
        @Path("id") postId: Int,
        @Body request: CreatePostRequest
    ): ApiResponse<Unit>

    @DELETE("/api/posts/{id}")
    suspend fun deletePost(
        @Header("Token") token: String,
        @Path("id") postId: Int
    ): ApiResponse<Unit>

    @POST("/api/posts/{id}/like")
    suspend fun likePost(
        @Header("Token") token: String,
        @Path("id") postId: Int
    ): ApiResponse<Map<String, Int>>

    @DELETE("/api/posts/{id}/like")
    suspend fun unlikePost(
        @Header("Token") token: String,
        @Path("id") postId: Int
    ): ApiResponse<Unit>

    @POST("/api/posts/{id}/favorite")
    suspend fun favoritePost(
        @Header("Token") token: String,
        @Path("id") postId: Int
    ): ApiResponse<Unit>

    @POST("/api/posts/{id}/coin")
    suspend fun coinPost(
        @Header("Token") token: String,
        @Path("id") postId: Int,
        @Body amount: Map<String, Int>
    ): ApiResponse<Unit>

    // 评论相关
    @POST("/api/comments/create")
    suspend fun createComment(
        @Header("Token") token: String,
        @Body request: CreateCommentRequest
    ): ApiResponse<Comment>

    @GET("/api/comments/list")
    suspend fun getCommentList(
        @Header("Token") token: String,
        @Query("post_id") postId: Int,
        @Query("sort") sort: String = "default",
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 50
    ): ApiResponse<CommentListResponse>

    @PUT("/api/comments/{id}")
    suspend fun updateComment(
        @Header("Token") token: String,
        @Path("id") commentId: Int,
        @Body content: Map<String, String>
    ): ApiResponse<Unit>

    @DELETE("/api/comments/{id}")
    suspend fun deleteComment(
        @Header("Token") token: String,
        @Path("id") commentId: Int
    ): ApiResponse<Unit>

    @POST("/api/comments/{id}/like")
    suspend fun likeComment(
        @Header("Token") token: String,
        @Path("id") commentId: Int
    ): ApiResponse<Unit>

    @POST("/api/comments/{id}/coin")
    suspend fun coinComment(
        @Header("Token") token: String,
        @Path("id") commentId: Int,
        @Body amount: Map<String, Int>
    ): ApiResponse<Map<String, Int>>

    // 收藏夹相关API
    @POST("/api/folders/create")
    suspend fun createFolder(
        @Header("Token") token: String,
        @Body request: CreateFolderRequest
    ): ApiResponse<CreateFolderResponse>

    @GET("/api/folders/my")
    suspend fun getMyFolders(@Header("Token") token: String): ApiResponse<List<Folder>>

    @POST("/api/folders/{id}/posts")
    suspend fun addPostToFolder(
        @Header("Token") token: String,
        @Path("id") folderId: Int,
        @Body request: Map<String, Int>
    ): ApiResponse<Unit>

    @GET("/api/comments/{id}/replies")
    suspend fun getCommentReplies(
        @Header("Token") token: String,
        @Path("id") commentId: Int,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20
    ): ApiResponse<CommentListResponse>

    // 板块创建API
    @POST("/api/boards/create")
    suspend fun createBoard(
        @Header("Token") token: String,
        @Body request: CreateBoardRequest
    ): ApiResponse<Board>
}

