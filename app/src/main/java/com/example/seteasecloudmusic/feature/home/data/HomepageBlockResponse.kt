package com.example.seteasecloudmusic.feature.home.data


/**
 * GET /homepage/block/page 顶层响应模型
 */
data class HomepageBlockPageResponse(
    val code: Int = 0,
    val message: String? = null,
    val data: HomepageBlockDataResponse? = null
)

data class HomepageBlockDataResponse(
    val cursor: String? = null,
    val hasMore: Boolean? = false,
    val blockCodeOrderList: List<String> = emptyList(),
    val blocks: List<HomepageBlockItemResponse> = emptyList()
)

data class HomepageBlockItemResponse(
    val blockCode: String = "",
    val showType: String? = null,
    val action: String? = null,
    val actionType: String? = null,
    val uiElement: HomepageBlockUiElementResponse? = null,
    val creatives: List<HomepageCreativeItemResponse> = emptyList()
)

data class HomepageBlockUiElementResponse(
    val subTitle: HomepageTitleItemResponse? = null,
    val mainTitle: HomepageTitleItemResponse? = null,
    val button: HomepageButtonItemResponse? = null
)

data class HomepageTitleItemResponse(
    val title: String? = null,
    val canShowTitleLogo: Boolean? = false,
    val titleType: String? = null,
    val titleId: String? = null
)

data class HomepageButtonItemResponse(
    val action: String? = null,
    val actionType: String? = null,
    val text: String? = null,
    val iconUrl: String? = null
)

data class HomepageCreativeItemResponse(
    val creativeType: String? = null,
    val uiElement: HomepageBlockUiElementResponse? = null,
    val resources: List<HomepageResourceItemResponse> = emptyList()
)

data class HomepageResourceItemResponse(
    val resourceType: String? = null,
    val resourceId: String? = null,
    val uiElement: HomepageResourceUiElementResponse? = null,
    val resourceExtInfo: HomepageResourceExtInfoResponse? = null,
    val action: String? = null,
    val actionType: String? = null
)

data class HomepageResourceUiElementResponse(
    val mainTitle: HomepageTitleItemResponse? = null,
    val subTitle: HomepageTitleItemResponse? = null,
    val image: HomepageImageItemResponse? = null,
    val rcmdShowType: String? = null
)

data class HomepageImageItemResponse(
    val imageUrl: String? = null,
    val purePicture: Boolean? = false
)

data class HomepageResourceExtInfoResponse(
    val artists: List<HomepageArtistItemResponse> = emptyList(),
    val songData: HomepageSongDataResponse? = null,
    val songPrivilege: HomepageSongPrivilegeResponse? = null
)

data class HomepageArtistItemResponse(
    val id: Long? = 0L,
    val name: String? = null,
    val picUrl: String? = null,
    val img1v1Url: String? = null
)

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

data class HomepageAlbumItemResponse(
    val id: Long? = 0L,
    val name: String? = null,
    val picUrl: String? = null,
    val blurPicUrl: String? = null
)

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

