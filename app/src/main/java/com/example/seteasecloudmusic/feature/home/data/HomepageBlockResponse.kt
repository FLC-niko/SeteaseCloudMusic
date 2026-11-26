package com.example.seteasecloudmusic.feature.home.data


/**
 * GET /homepage/block/page 顶层响应模型
 */
data class HomepageBlockPageResponse(
    val code: Int = 0,
    val message: String? = null,
    val data: HomepageBlockDataResponse? = null
)

/** 首页分页数据主体，包含游标与区块列表。 */
data class HomepageBlockDataResponse(
    val cursor: String? = null,
    val hasMore: Boolean? = false,
    val blockCodeOrderList: List<String> = emptyList(),
    val blocks: List<HomepageBlockItemResponse> = emptyList()
)

/** 首页单个功能区块（按 blockCode 区分模块类型）。 */
data class HomepageBlockItemResponse(
    val blockCode: String = "",
    val showType: String? = null,
    val action: String? = null,
    val actionType: String? = null,
    val uiElement: HomepageBlockUiElementResponse? = null,
    val creatives: List<HomepageCreativeItemResponse> = emptyList()
)

/** 区块级别的 UI 展示元素（标题与操作按钮）。 */
data class HomepageBlockUiElementResponse(
    val subTitle: HomepageTitleItemResponse? = null,
    val mainTitle: HomepageTitleItemResponse? = null,
    val button: HomepageButtonItemResponse? = null
)

/** 区块或资源的标题文案结构。 */
data class HomepageTitleItemResponse(
    val title: String? = null,
    val canShowTitleLogo: Boolean? = false,
    val titleType: String? = null,
    val titleId: String? = null
)

/** 区块头部操作按钮配置。 */
data class HomepageButtonItemResponse(
    val action: String? = null,
    val actionType: String? = null,
    val text: String? = null,
    val iconUrl: String? = null
)

/** 区块内的创意编排分组，承载多个资源条目。 */
data class HomepageCreativeItemResponse(
    val creativeType: String? = null,
    val uiElement: HomepageBlockUiElementResponse? = null,
    val resources: List<HomepageResourceItemResponse> = emptyList()
)

/** 区块内的单个资源条目（歌曲或歌单引用）。 */
data class HomepageResourceItemResponse(
    val resourceType: String? = null,
    val resourceId: String? = null,
    val uiElement: HomepageResourceUiElementResponse? = null,
    val resourceExtInfo: HomepageResourceExtInfoResponse? = null,
    val action: String? = null,
    val actionType: String? = null
)

/** 资源条目的 UI 展示信息（主副标题与封面图）。 */
data class HomepageResourceUiElementResponse(
    val mainTitle: HomepageTitleItemResponse? = null,
    val subTitle: HomepageTitleItemResponse? = null,
    val image: HomepageImageItemResponse? = null,
    val rcmdShowType: String? = null
)

/** 资源封面图信息。 */
data class HomepageImageItemResponse(
    val imageUrl: String? = null,
    val purePicture: Boolean? = false
)

/** 资源条目的扩展信息（歌曲实体、歌手与权限）。 */
data class HomepageResourceExtInfoResponse(
    val artists: List<HomepageArtistItemResponse> = emptyList(),
    val songData: HomepageSongDataResponse? = null,
    val songPrivilege: HomepageSongPrivilegeResponse? = null,
    val playCount: Long? = null
)

/** 首页推荐曲目中的歌手信息。 */
data class HomepageArtistItemResponse(
    val id: Long? = 0L,
    val name: String? = null,
    val picUrl: String? = null,
    val img1v1Url: String? = null
)

/** 首页区块内嵌的歌曲基础数据与音质标识。 */
data class HomepageSongDataResponse(
    val id: Long = 0L,
    val name: String? = null,
    val duration: Long? = 0L,
    val fee: Int? = 0,
    val artists: List<HomepageArtistItemResponse> = emptyList(),
    val album: HomepageAlbumItemResponse? = null,
    val sqMusic: Any? = null,
    val hrMusic: Any? = null,
    val hMusic: Any? = null,
    val mMusic: Any? = null,
    val lMusic: Any? = null
)

/** 首页区块歌曲的专辑信息。 */
data class HomepageAlbumItemResponse(
    val id: Long? = 0L,
    val name: String? = null,
    val picUrl: String? = null,
    val blurPicUrl: String? = null
)

/** 首页区块歌曲的播放权限信息。 */
data class HomepageSongPrivilegeResponse(
    val id: Long? = 0L,
    val fee: Int? = 0,
    val st: Int? = 0,
    val pl: Long? = 0L,
    val dl: Long? = 0L,
    val sp: Int? = 0,
    val cp: Int? = 0,
    val subp: Int? = 0,
    val maxbr: Long? = 0L
)

