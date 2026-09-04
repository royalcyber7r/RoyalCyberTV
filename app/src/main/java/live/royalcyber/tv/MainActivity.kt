package live.royalcyber.tv

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.ui.PlayerView

import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView


class MainActivity : AppCompatActivity() {

    private lateinit var playerView: PlayerView
    private lateinit var playerContainer: View

    private lateinit var channelRecycler: RecyclerView
    private lateinit var channelAdapter: ChannelAdapter

    private lateinit var searchButton: ImageButton
    private lateinit var searchBox: EditText
    private lateinit var fullscreenButton: ImageButton

    private lateinit var mainScrollView: ScrollView
    private lateinit var headerLayout: View
    private lateinit var currentChannelName: TextView
    private lateinit var channelTitle: TextView
    private lateinit var footerLayout: View
    private lateinit var bottomNavigation: View

    private lateinit var footerFacebook: ImageView
    private lateinit var footerYoutube: ImageView
    private lateinit var footerInstagram: ImageView
    private lateinit var footerTiktok: ImageView

    /* ================= PLAYER CONTROLS ================= */

    private lateinit var playerControls: View
    private lateinit var playPauseButton: TextView
    private lateinit var rewindButton: TextView
    private lateinit var forwardButton: TextView
    private lateinit var liveText: TextView

    private var player: ExoPlayer? = null

    private var currentChannel: Channel? = null

    private var isFullscreen = false

    private var normalPlayerHeight = 220

    private var searchWasVisible = false

    /* =========================================================
       BOTTOM NAVIGATION BASE HEIGHT
       ========================================================= */

    private var bottomNavigationBaseHeight = 70


    /* =========================================================
       UPDATE SYSTEM
       ========================================================= */

    private var updateCheckStarted = false

    private val handler =
        Handler(Looper.getMainLooper())

    private val hideControlsRunnable =
        Runnable {

            if (!isFullscreen) {

                playerControls.visibility =
                    View.GONE
            }
        }


    /* =========================================================
       CHANNEL LIST
       ========================================================= */

    private val channels = listOf(

        /* =====================================================
           🇧🇩 BANGLADESHI CHANNELS
           ===================================================== */

        Channel(
            name = "Global Tv",
            logo = "https://globaltvbd.com/storage/settings/01KVWJD176R6VT9GC3B9KEJBWK.png",
            streamUrl = "https://stream.ottplus.live/live/global_tv_abr/index.m3u8"
        ),

        Channel(
            name = "Thikana Tv",
            logo = "https://web.aynaott.com/storage/019dd92f-107c-7056-9e79-e5233f6e51d9/uploads/images/2026-07-02/images_b9a8bdbfcea2fb4656204d06f615682a_playmist_thikana400x400.jpg",
            streamUrl = "https://5dd3981940faa.streamlock.net:443/thikanatv/thikanatv/playlist.m3u8"
        ),

        Channel(
            name = "Drama 24",
            logo = "https://yt3.googleusercontent.com/ytc/AIdro_mRNcwLGFRiDadXg634lWGLZRX94k4kFVCQne23qV2b-G0=w544-c-h544-k-c0x00ffffff-no-l90-rj",
            streamUrl = "https://vods2.aynaott.com/gseriesDrama/index.m3u8"
        ),

        Channel(
            name = "Joo Music",
            logo = "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRbNd6fN0L21ZYgbAtkKGaS-t-cqTGfbk0svw&s",
            streamUrl = "https://livecdn.live247stream.com/joomusic/tv/playlist.m3u8"
        ),

        Channel(
            name = "Jamuna TV",
            logo = "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQpAd7TpOPf8Jbo71Y9Pke4Y7APhCsQu0sJeV121SbGzErsYmogJaf9SZs&s=10",
            streamUrl = "https://stream.ottplus.live/live/jamuna_tv_abr/index.m3u8"
        ),

        Channel(
            name = "ATN Music",
            logo = "https://yt3.googleusercontent.com/fl29COpVoSRu4iXG505zsyWag0d9kdB-I-C2mm8h7ePDiT2SSTqGWyt93ObOHWcLdh0wa7yd=s900-c-k-c0x00ffffff-no-rj",
            streamUrl = "https://app.ncare.live/c3VydmVyX8RpbEU9Mi8xNy8yMDE0GIDU6RgzQ6NTAgdEoaeFzbF92YWxIZTO0U0ezN1IzMyfvcGVMZEJCTEFWeVN3PTOmdFsaWRtaW51aiPhnPTI/atnmusic.stream/playlist.m3u8"
        ),

        Channel(
            name = "Channel S",
            logo = "https://raw.githubusercontent.com/royalcyber7r/RoyalCyberTV/main/app/src/main/logos/Channel_S.png",
            streamUrl = "https://app.ncare.live/c3VydmVyX8RpbEU9Mi8xNy8yMDE0GIDU6RgzQ6NTAgdEoaeFzbF92YWxIZTO0U0ezN1IzMyfvcGVMZEJCTEFWeVN3PTOmdFsaWRtaW51aiPhnPTI2/channels.stream/live-orgin/channels.stream/playlist.m3u8"
        ),

        Channel(
            name = "Ekhon TV",
            logo = "https://raw.githubusercontent.com/royalcyber7r/RoyalCyberTV/main/app/src/main/logos/ekhon.jpg",
            streamUrl = "https://tvsen6.aynaott.com/fbgZV3X17hwWcyfZ4pdb/index.m3u8"
        ),

        Channel(
            name = "Nexus TV",
            logo = "https://yt3.googleusercontent.com/acZUkF66bm_ut3x__Ut9lCkfc8UXAR-IvsKEqQk_bEgyceypzynXcdR65e9rVnKdGRWsSRRgOg=s900-c-k-c0x00ffffff-no-rj",
            streamUrl = "https://tvsen6.aynaott.com/Epm7WrFa/index.m3u8"
        ),

        Channel(
            name = "NTV",
            logo = "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRBajLCVfnTQd8X3xf8XZkLJIzNKdj35CqJww&s",
            streamUrl = "https://tvsen5.aynaott.com/xV4jEKf3D9zc/tracks-v1a1/mono.ts.m3u8"
        ),

        Channel(
            name = "RTV",
            logo = "https://www.jagobd.com/wp-content/uploads/2017/01/rtvbd.jpg?x95285",
            streamUrl = "https://app24.jagobd.com.bd/c3VydmVyX8RpbEU9Mi8xNy8yMFDEEHGcfRgzQ6NTAgdEoaeFzbF92YWxIZTO0U0ezN1IzMyfvcEdsEfeDeKiNkVN3PTOmdFseWRtaW51aiPhnPTI2/rtv-sg.stream/index.m3u8"
        ),

        Channel(
            name = "Deepto TV",
            logo = "https://raw.githubusercontent.com/royalcyber7r/RoyalCyberTV/main/app/src/main/logos/Deepto_TV.webp",
            streamUrl = "https://byphdgllyk.gpcdn.net/hls/deeptotv/0_1/index.m3u8"
        ),

        Channel(
            name = "My TV",
            logo = "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcSTwzEuhaRG7YKVDoXfXDYcdlvNkShrJje8Em3lzCPghg&s",
            streamUrl = "https://tvsen6.aynaott.com/XMpHaEf0ANBhv8w6NWR7/index.m3u8"
        ),

        Channel(
            name = "Maasranga TV HD",
            logo = "https://raw.githubusercontent.com/royalcyber7r/RoyalCyberTV/main/app/src/main/logos/Maasranga_Television.jpg",
            streamUrl = "https://tvsen5.aynaott.com/maasrangatv/index.m3u8"
        ),

        Channel(
            name = "Channel i HD",
            logo = "https://tstatic.akash-go.com/cms-ui/images/custom-content/1740567626692.png",
            streamUrl = "https://tvsen6.aynaott.com/FNHpYvGZ7FkCE10PwTHm/index.m3u8"
        ),

        Channel(
            name = "Desh TV",
            logo = "https://raw.githubusercontent.com/royalcyber7r/RoyalCyberTV/main/app/src/main/logos/deshtv.jpg",
            streamUrl = "https://tvsen6.aynaott.com/ryFkXfd1a4CQ7mMdc820/index.m3u8"
        ),

        Channel(
            name = "Ananda TV",
            logo = "https://www.jagobd.com/wp-content/uploads/2018/04/Anandatvupdate.jpg?x95285",
            streamUrl = "https://tvsen6.aynaott.com/LeUAm4F1iixYns3s3Non/index.m3u8"
        ),

        Channel(
            name = "Ekushey TV HD",
            logo = "https://s4.gifyu.com/images/image534fa27d7683f33d.png",
            streamUrl = "https://tvsen6.aynaott.com/y4mEVZNAbeNWTbd6Z2Pw/index.m3u8"
        ),

        Channel(
            name = "Asian TV HD",
            logo = "https://assets-prod.services.toffeelive.com/MyK__poBEef-9-uVmf5l/posters/1eadef5b-28e7-4dc2-b42f-c67a3357c9a0.png",
            streamUrl = "https://stream.ottplus.live/live/asian_tv_abr/index.m3u8"
        ),

        Channel(
            name = "Boishakhi TV",
            logo = "https://www.jagobd.com/wp-content/uploads/2015/10/BoishakhiTV-150x1501.jpg?x95285",
            streamUrl = "https://tvsen6.aynaott.com/1d3uG9VCgrR9DRtWZM57/index.m3u8"
        ),

        Channel(
            name = "Sangeet Bangla HD",
            logo = "https://yt3.googleusercontent.com/FGx9xqm5eU1DZXDk4ZDRQDK9fyhvZ2LR6gKXhZcJeFunvG9SwT8SB01SxbiD3GDL8MqMKxWXHQ=s900-c-k-c0x00ffffff-no-rj",
            streamUrl = "https://cdn-4.pishow.tv/live/1143/master.m3u8"
        ),

        Channel(
            name = "ATN News HD",
            logo = "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcR_PzLYrDkO7qND7MCx4Tk_awS9J9PwOzcH6Q&s=",
            streamUrl = "https://tvsen6.aynaott.com/da6WMXAk/index.m3u8"
        ),

        Channel(
            name = "DeshBidesh",
            logo = "https://i.imgur.com/Ek1Ohj6.png",
            streamUrl = "https://dbcanada.sonarbanglatv.com/deshebideshe/dbtv/index.m3u8"
        ),

        Channel(
            name = "Ekator TV HD",
            logo = "https://s4.gifyu.com/images/imagea02f4314e761661d.png",
            streamUrl = "https://tvsen6.aynaott.com/EWDrV5QskgarZEUBb3pU/index.m3u8"
        ),

        Channel(
            name = "Channel 24 HD",
            logo = "https://www.jagobd.com/wp-content/uploads/2016/02/channel24.jpg?x95285",
            streamUrl = "https://stream.ottplus.live/live/channel_24_abr/index.m3u8"
        ),

        Channel(
            name = "Channel 1 4K",
            logo = "https://www.thedailystar.net/sites/default/files/styles/big_1/public/images/2025/02/24/channel_1.png",
            streamUrl = "https://stream.ottplus.live/live/channel_1_hd_abr/index.m3u8"
        ),

        Channel(
            name = "DBC News",
            logo = "https://tstatic.akash-go.com/cms-ui/images/custom-content/1770186306600.png",
            streamUrl = "https://owrcovcrpy.gpcdn.net/bpk-tv/1728/output/index.m3u8"
        ),

        Channel(
            name = "News 24 HD",
            logo = "https://tstatic.akash-go.com/cms-ui/images/custom-content/1770186895850.png",
            streamUrl = "https://owrcovcrpy.gpcdn.net/bpk-tv/1708/output/index.m3u8"
        ),

        Channel(
            name = "BTV HD",
            logo = "https://i.pinimg.com/736x/c3/8c/b7/c38cb7aa28d273128b42f70a428e611e.jpg",
            streamUrl = "https://tvsen6.aynaott.com/TjGR1GcxKetHNVcMVxbq/index.m3u8"
        ),

        Channel(
            name = "BTV Sangsad",
            logo = "https://upload.wikimedia.org/wikipedia/en/thumb/a/a6/Sangsad_Television_Emblem.svg/250px-Sangsad_Television_Emblem.svg.png?utm_source=en.wikipedia.org&utm_campaign=parser&utm_content=thumbnail",
            streamUrl = "https://owrcovcrpy.gpcdn.net/bpk-tv/1709/output/index.m3u8"
        ),

        Channel(
            name = "SA TV HD",
            logo = "https://tstatic.akash-go.com/cms-ui/images/custom-content/1770187361105.png",
            streamUrl = "https://tvsen6.aynaott.com/rELXiuUXqbgzPb06Npom/index.m3u8"
        ),

        Channel(
            name = "Green TV",
            logo = "https://www.jagobd.com/wp-content/uploads/2022/12/green-tv.jpg",
            streamUrl = "https://app.ncare.live/c3VydmVyX8RpbEU9Mi8xNy8yMDE0GIDU6RgzQ6NTAgdEoaeFzbF92YWxIZTO0U0ezN1IzMyfvcGVMZEJCTEFWeVN3PTOmdFsaWRtaW51aiPhnPTI2/greentv.stream/live-orgin/greentv.stream/playlist.m3u8"
        ),

        /* =====================================================
           ⚽ SPORTS CHANNELS
           ===================================================== */

        Channel(
            name = "T Sports",
            logo = "https://yt3.googleusercontent.com/IFgAG_o_AdtX4IauErKIzuFGCj0m4QyH81Q1Uq8H-2Si9ul3vmXkLihDUnn6-QI3xiMZech0AQ=s900-c-k-c0x00ffffff-no-rj",
            streamUrl = "https://tvsen5.aynaott.com/TnMn5kZz8aLm/index.m3u8"
        ),

        Channel(
            name = "A Sports",
            logo = "https://raw.githubusercontent.com/royalcyber7r/RoyalCyberTV/main/app/src/main/logos/A_Sports.png",
            streamUrl = "https://tvsen6.aynaott.com/zv68oqPDu7MZZwmHhRxt/index.m3u8"
        ),

        Channel(
            name = "Bein Sports Direct",
            logo = "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQkuwIi4KCSdEc0i8OLMZSEhUkAzkd6cWArxA&s",
            streamUrl = "https://1nyaler.streamhostingcdn.top/stream/23/index.m3u8"
        ),

        Channel(
            name = "TNT Sports ARG Premium",
            logo = "https://upload.wikimedia.org/wikipedia/commons/f/f4/TNT_Sports_Premium_%28Argentina%29.png",
            streamUrl = "https://1nyaler.streamhostingcdn.top/stream/30/index.m3u8"
        ),

        Channel(
            name = "Red Bull TV",
            logo = "https://upload.wikimedia.org/wikipedia/vi/thumb/6/6d/Red_Bull_Logo.svg/3840px-Red_Bull_Logo.svg.png",
            streamUrl = "https://rbmn-live.akamaized.net/hls/live/590964/BoRB-AT/master.m3u8"
        ),

        Channel(
            name = "DD Sports",
            logo = "https://raw.githubusercontent.com/royalcyber7r/RoyalCyberTV/main/app/src/main/logos/ddsport.jpg",
            streamUrl = "https://d3qs3d2rkhfqrt.cloudfront.net/out/v1/b17adfe543354fdd8d189b110617cddd/index.m3u8"
        ),

        Channel(
            name = "TPV Sport",
            logo = "https://raw.githubusercontent.com/royalcyber7r/RoyalCyberTV/main/app/src/main/logos/tvp.png",
            streamUrl = "https://1nyaler.streamhostingcdn.top/stream/89/index.m3u8"
        ),

        /* =====================================================
           🇮🇳 INDIAN CHANNELS
           ===================================================== */

        Channel(
            name = "TV9 Bangla",
            logo = "https://static.wikia.nocookie.net/logopedia/images/2/2b/Tv9bangla.png/revision/latest/scale-to-width-down/280?cb=20210509173815",
            streamUrl = "https://dyjmyiv3bp2ez.cloudfront.net/pub-iotv9banaen8yq/liveabr/playlist.m3u8"
        ),

        Channel(
            name = "Z Cenema",
            logo = "https://raw.githubusercontent.com/royalcyber7r/RoyalCyberTV/main/app/src/main/logos/z-cenema.jpg",
            streamUrl = "https://d1g8wgjurz8via.cloudfront.net/bpk-tv/NGCHD/default/NGCHD.m3u8"
        ),

        Channel(
            name = "Sony Aath",
            logo = "https://upload.wikimedia.org/wikipedia/en/6/64/Sony_Aath_Logo.png",
            streamUrl = "https://live20.bozztv.com/giatvplayout7/giatv-209611/index.m3u8"
        ),

        Channel(
            name = "Zee 24 Ghanta HD",
            logo = "https://i.postimg.cc/tTNPLBMs/24-Ghanta.jpg",
            streamUrl = "https://d2dsoyvkr33m05.cloudfront.net/index_1.m3u8"
        ),

        Channel(
            name = "Goldmines Movies HD",
            logo = "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRUso-0xopQb02-1w2nw5xxHpydkdNNFS5Cqg&s",
            streamUrl = "https://cdn-2.pishow.tv/live/1461/master.m3u8"
        ),

        Channel(
            name = "9XM",
            logo = "https://yt3.googleusercontent.com/a5SfL3aMu1G_MufbVoDv0wPz4gcyn_pYtsR3BAMH08B368gh-ytxzBWgPpdmKyWemwCGF0Ql=s900-c-k-c0x00ffffff-no-rj",
            streamUrl = "https://9xjio.wiseplayout.com/9XM/master.m3u8"
        ),

        Channel(
            name = "Yrf Music HD",
            logo = "https://jiotvimages.cdn.jio.com/dare_images/images/channel/756c50edae8599fb760cbbfb22010a75.png",
            streamUrl = "https://cdn-uw2-prod.tsv2.amagi.tv/linear/amg01412-xiaomiasia-yrfmusic-xiaomi/playlist.m3u8"
        ),

        Channel(
            name = "Music India HD",
            logo = "https://static.wikia.nocookie.net/logopedia/images/2/2f/Music_India.jpeg",
            streamUrl = "https://cdn-2.pishow.tv/live/226/master.m3u8"
        ),

        Channel(
            name = "Eros Now",
            logo = "https://upload.wikimedia.org/wikipedia/commons/f/fe/ErosNow_Stag_New_18_White.jpg?utm_source=commons.wikimedia.org&utm_campaign=index&utm_content=original",
            streamUrl = "https://live20.bozztv.com/giatvplayout7/giatv-209612/tracks-v1a1/mono.ts.m3u8"
        ),

        Channel(
            name = "WAM",
            logo = "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTvrI0UZWtzXNzpIdpiJmKFbgVDUnMr0DkQyvrnjTbt5DQDwsxPGjvASR8&s=10",
            streamUrl = "https://live20.bozztv.com/giatvplayout7/giatv-209593/tracks-v1a1/mono.ts.m3u8"
        ),

        Channel(
            name = "India Today",
            logo = "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQZw3lZN_0pLcMA5ZUhuDEc6oDRgPbaEZGtRA&s",
            streamUrl = "https://indiatodaylive.akamaized.net/hls/live/2014320/indiatoday/indiatodaylive/playlist.m3u8"
        ),

        Channel(
            name = "SRK TV",
            logo = "https://tstatic.akash-go.com/cms-ui/images/custom-content/1746005940155.png",
            streamUrl = "https://srknowapp.ncare.live/srktvhlswodrm/srktv.stream/playlist.m3u8"
        ),

        Channel(
            name = "DD Bangla HD",
            logo = "https://i.postimg.cc/WzhwJYDJ/DD-Bangla.jpg",
            streamUrl = "https://d3qs3d2rkhfqrt.cloudfront.net/out/v1/7ff57cc9046b4c188b51a0d506f36e7f/index_3.m3u8"
        ),

        Channel(
            name = "Hindi Movie Classic 24",
            logo = "https://s3.aynaott.com/storage/3132515182ec50091b496fe515564084",
            streamUrl = "https://vods2.aynaott.com/hindimovies/index.m3u8"
        ),

        Channel(
            name = "R Plus Gold",
            logo = "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQEqtnqPJ3ELtDpyyNJCWYyOz--NgOs7kQADM5g7XW44v7N9oyXPGQXraY&s=10",
            streamUrl = "https://cdn-4.pishow.tv/live/1231/1231_1.m3u8"
        ),

        /* =====================================================
           🌍 INTERNATIONAL CHANNELS
           ===================================================== */

        Channel(
            name = "Net Tv",
            logo = "https://i.imgur.com/EWmshtx.png",
            streamUrl = "https://unlimited1-us.dps.live/nettv/nettv.smil/playlist.m3u8"
        ),

        Channel(
            name = "Deluxe Music",
            logo = "https://raw.githubusercontent.com/royalcyber7r/RoyalCyberTV/main/app/src/main/logos/Deluxe-music.png",
            streamUrl = "https://sdn-global-live-streaming-packager-cache.3qsdn.com/13456/13456_264_live.m3u8"
        ),

        Channel(
            name = "Retro Music",
            logo = "https://raw.githubusercontent.com/royalcyber7r/RoyalCyberTV/main/app/src/main/logos/retro-music.jpg",
            streamUrl = "https://stream.mediawork.cz/retrotv/smil:retrotv2.smil/playlist.m3u8"
        ),

        Channel(
            name = "EBS Kids",
            logo = "https://raw.githubusercontent.com/royalcyber7r/RoyalCyberTV/main/app/src/main/logos/ebs-kids.jpg",
            streamUrl = "https://ebsonair.ebs.co.kr/ebsufamilypc/familypc1m/playlist.m3u8"
        ),

        Channel(
            name = "Nickjr",
            logo = "https://raw.githubusercontent.com/royalcyber7r/RoyalCyberTV/main/app/src/main/logos/Nick Jr.jpg",
            streamUrl = "https://tvsen5.aynaott.com/nickjr/index.m3u8"
        ),

        Channel(
            name = "Al Arabiya Al Hadath HD",
            logo = "https://raw.githubusercontent.com/royalcyber7r/RoyalCyberTV/main/app/src/main/logos/al_arabiya.png",
            streamUrl = "https://live.alarabiya.net/alarabiapublish/alhadath.smil/alarabiapublish/alhadath_720p/chunks.m3u8"
        ),

        Channel(
            name = "Action Hollywood Movies",
            logo = "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcR1S-kP0WIjZAjBtqhr-g7dYQl5rudjKOK6vkQJRNpwyENYrqHa0Y7MnHiI&s=10",
            streamUrl = "https://amg01076-lightningintern-actionhollywood-samsungnz-82rry.amagi.tv/playlist/amg01076-lightningintern-actionhollywood-samsungnz/playlist.m3u8"
        ),

        Channel(
            name = "CNN USA",
            logo = "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcSk5qbYrWblEWdFFAOZa3KaAlnPTvlGayE_mA&s",
            streamUrl = "https://turnerlive.warnermediacdn.com/hls/live/586495/cnngo/cnn_slate/VIDEO_0_3564000.m3u8"
        ),

        Channel(
            name = "Global News",
            logo = "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcS9hVBEVr65a4nsYDIOuY9tvDL4NWe9Bfaz7g&s",
            streamUrl = "https://live.corusdigitaldev.com/groupd/live/49a91e7f-1023-430f-8d66-561055f3d0f7/live.isml/.m3u8"
        ),

        Channel(
            name = "Modina Live",
            logo = "https://images-na.ssl-images-amazon.com/images/I/71CywdrFaZL.png",
            streamUrl = "https://cdn-globecast.akamaized.net/live/eds/saudi_sunnah/hls_roku/index.m3u8"
        )

    ).distinctBy {

        it.name
            .trim()
            .lowercase()
    }


    /* =========================================================
       ON CREATE
       ========================================================= */

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_main
        )

        initializeViews()

        setupBottomNavigationInsets()

        setupChannelList()

        setupSearch()

        setupPlayer()

        setupPlayerControls()

        setupFullscreenButton()

        setupBottomMenu()

        setupSocialLinks()

        mainScrollView.post {

            updateRecyclerHeight()
        }

        if (channels.isNotEmpty()) {

            playChannel(
                channels[0]
            )
        }

        /*
         * App-এর মূল UI আগে সম্পূর্ণ layout হতে দেওয়া হচ্ছে।
         * তারপর UpdateActivity চালু হবে।
         */
        handler.postDelayed({

            if (
                !isFinishing &&
                !isDestroyed
            ) {

                checkForUpdateAutomatically()
            }

        }, 1500)
    }


    /* =========================================================
       BOTTOM NAVIGATION SYSTEM INSETS
       ========================================================= */

    private fun setupBottomNavigationInsets() {

        if (!::bottomNavigation.isInitialized) {
            return
        }

        val density =
            resources
                .displayMetrics
                .density

        bottomNavigationBaseHeight =
            (
                70f * density
            ).toInt()

        ViewCompat.setOnApplyWindowInsetsListener(
            bottomNavigation
        ) { view, insets ->

            val navigationInsets =
                insets.getInsets(
                    WindowInsetsCompat.Type.navigationBars()
                )

            val bottomInset =
                navigationInsets.bottom

            val params =
                view.layoutParams

            params.height =
                bottomNavigationBaseHeight +
                    bottomInset

            view.layoutParams =
                params

            view.setPadding(
                view.paddingLeft,
                0,
                view.paddingRight,
                bottomInset
            )

            insets
        }

        ViewCompat.requestApplyInsets(
            bottomNavigation
        )
    }


    /* =========================================================
       AUTOMATIC UPDATE
       ========================================================= */

    private fun checkForUpdateAutomatically() {

        if (updateCheckStarted) {
            return
        }

        updateCheckStarted = true

        try {

            val intent =
                Intent(
                    this,
                    UpdateActivity::class.java
                )

            startActivity(intent)

        } catch (
            _: Exception
        ) {

            updateCheckStarted = false
        }
    }


    /* =========================================================
       MANUAL UPDATE
       ========================================================= */

    private fun openUpdateScreen() {

        try {

            val intent =
                Intent(
                    this,
                    UpdateActivity::class.java
                )

            startActivity(intent)

        } catch (
            _: Exception
        ) {

            Toast.makeText(
                this,
                "Update System চালু করা যাচ্ছে না",
                Toast.LENGTH_SHORT
            ).show()
        }
    }


    /* =========================================================
       INITIALIZE
       ========================================================= */

    private fun initializeViews() {

        playerView =
            findViewById(
                R.id.player_view
            )

        playerContainer =
            findViewById(
                R.id.player_container
            )

        channelRecycler =
            findViewById(
                R.id.channel_recycler
            )

        searchButton =
            findViewById(
                R.id.search_button
            )

        searchBox =
            findViewById(
                R.id.search_box
            )

        fullscreenButton =
            findViewById(
                R.id.fullscreen_button
            )

        mainScrollView =
            findViewById(
                R.id.main_scroll_view
            )

        headerLayout =
            findViewById(
                R.id.header_layout
            )

        currentChannelName =
            findViewById(
                R.id.current_channel_name
            )

        channelTitle =
            findViewById(
                R.id.channel_title
            )

        footerLayout =
            findViewById(
                R.id.footer_layout
            )

        bottomNavigation =
            findViewById(
                R.id.bottom_navigation
            )

        footerFacebook =
            findViewById(
                R.id.footer_facebook
            )

        footerYoutube =
            findViewById(
                R.id.footer_youtube
            )

        footerInstagram =
            findViewById(
                R.id.footer_instagram
            )

        footerTiktok =
            findViewById(
                R.id.footer_tiktok
            )

        playerControls =
            findViewById(
                R.id.player_controls
            )

        playPauseButton =
            findViewById(
                R.id.play_pause_button
            )

        rewindButton =
            findViewById(
                R.id.rewind_button
            )

        forwardButton =
            findViewById(
                R.id.forward_button
            )

        liveText =
            findViewById(
                R.id.live_text
            )
    }


    /* =========================================================
       CHANNEL LIST
       ========================================================= */

    private fun setupChannelList() {

        channelRecycler.layoutManager =
            GridLayoutManager(
                this,
                3
            )

        channelAdapter =
            ChannelAdapter(
                channels = channels,
                onChannelClick = { channel ->

                    playChannel(
                        channel
                    )
                }
            )

        channelRecycler.adapter =
            channelAdapter

        channelRecycler.isNestedScrollingEnabled =
            false

        channelRecycler.setHasFixedSize(
            false
        )

        updateRecyclerHeight()
    }


    private fun updateRecyclerHeight() {

        if (!::channelAdapter.isInitialized) {
            return
        }

        if (!::channelRecycler.isInitialized) {
            return
        }

        val itemCount =
            channelAdapter.itemCount

        val columns =
            3

        val rows =
            if (itemCount == 0) {

                0

            } else {

                (
                    itemCount +
                        columns -
                        1
                    ) / columns
            }

        val density =
            resources
                .displayMetrics
                .density

        val rowHeightDp =
            145

        val bottomPaddingDp =
            15

        val heightPx =
            (
                rows *
                    rowHeightDp +
                    bottomPaddingDp
                ) * density

        val params =
            channelRecycler.layoutParams

        params.height =
            heightPx.toInt()

        channelRecycler.layoutParams =
            params
    }


    /* =========================================================
       PLAYER SETUP
       ========================================================= */

    private fun setupPlayer() {

        player =
            ExoPlayer.Builder(
                this
            ).build()

        playerView.player =
            player

        playerView.useController =
            false

        playerView.keepScreenOn =
            true

        playerView.setShowBuffering(
            PlayerView.SHOW_BUFFERING_WHEN_PLAYING
        )

        player?.repeatMode =
            Player.REPEAT_MODE_ONE

        player?.addListener(
            object : Player.Listener {

                override fun onIsPlayingChanged(
                    isPlaying: Boolean
                ) {

                    updatePlayPauseButton(
                        isPlaying
                    )
                }


                override fun onPlaybackStateChanged(
                    playbackState: Int
                ) {

                    when (playbackState) {

                        Player.STATE_BUFFERING -> {

                            liveText.text =
                                "●  BUFFERING..."
                        }


                        Player.STATE_READY -> {

                            liveText.text =
                                "●  LIVE"
                        }


                        Player.STATE_ENDED -> {

                            liveText.text =
                                "●  LIVE"

                            resumePlayback()
                        }


                        Player.STATE_IDLE -> {
                        }
                    }
                }


                override fun onPlayerError(
                    error: PlaybackException
                ) {

                    Toast.makeText(
                        this@MainActivity,
                        "এই Channel চালু করা যাচ্ছে না",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        )
    }


    /* =========================================================
       PLAYER CONTROLS
       ========================================================= */

    private fun setupPlayerControls() {

        playPauseButton.setOnClickListener {

            val exoPlayer =
                player
                    ?: return@setOnClickListener

            when {

                exoPlayer.playbackState ==
                    Player.STATE_IDLE -> {

                    resumePlayback()
                }


                exoPlayer.playbackState ==
                    Player.STATE_ENDED -> {

                    resumePlayback()
                }


                exoPlayer.isPlaying -> {

                    exoPlayer.pause()
                }


                else -> {

                    exoPlayer.play()
                }
            }

            updatePlayPauseButton(
                exoPlayer.isPlaying
            )

            showControlsTemporarily()
        }


        rewindButton.setOnClickListener {

            val exoPlayer =
                player
                    ?: return@setOnClickListener

            exoPlayer.seekBack()

            showControlsTemporarily()
        }


        forwardButton.setOnClickListener {

            val exoPlayer =
                player
                    ?: return@setOnClickListener

            exoPlayer.seekForward()

            showControlsTemporarily()
        }


        playerView.setOnClickListener {

            if (
                playerControls.visibility ==
                    View.VISIBLE
            ) {

                playerControls.visibility =
                    View.GONE

                handler.removeCallbacks(
                    hideControlsRunnable
                )

            } else {

                playerControls.visibility =
                    View.VISIBLE

                showControlsTemporarily()
            }
        }


        updatePlayPauseButton(
            false
        )
    }


    /* =========================================================
       PLAY / PAUSE ICON
       ========================================================= */

    private fun updatePlayPauseButton(
        isPlaying: Boolean
    ) {

        if (!::playPauseButton.isInitialized) {
            return
        }

        playPauseButton.text =
            if (isPlaying) {

                "❚❚"

            } else {

                "▶"
            }
    }


    /* =========================================================
       CONTROL AUTO HIDE
       ========================================================= */

    private fun showControlsTemporarily() {

        handler.removeCallbacks(
            hideControlsRunnable
        )

        if (!isFullscreen) {

            handler.postDelayed(
                hideControlsRunnable,
                5000
            )
        }
    }


    /* =========================================================
       PLAY CHANNEL
       ========================================================= */

    private fun playChannel(
        channel: Channel
    ) {

        val url =
            channel.streamUrl.trim()

        if (url.isEmpty()) {

            Toast.makeText(
                this,
                "Stream URL পাওয়া যায়নি",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        currentChannel =
            channel

        val dataSourceFactory =
            DefaultHttpDataSource.Factory()
                .setAllowCrossProtocolRedirects(
                    true
                )

        val mediaSource =
            HlsMediaSource.Factory(
                dataSourceFactory
            ).createMediaSource(
                MediaItem.fromUri(
                    url
                )
            )

        player?.apply {

            stop()

            clearMediaItems()

            setMediaSource(
                mediaSource
            )

            prepare()

            playWhenReady =
                true

            play()
        }

        currentChannelName.text =
            channel.name

        liveText.text =
            "●  LIVE"

        playerControls.visibility =
            View.VISIBLE

        updatePlayPauseButton(
            true
        )

        showControlsTemporarily()

        if (!isFullscreen) {

            mainScrollView.post {

                mainScrollView.smoothScrollTo(
                    0,
                    0
                )
            }
        }
    }


    /* =========================================================
       RESUME
       ========================================================= */

    private fun resumePlayback() {

        val exoPlayer =
            player
                ?: return

        if (
            exoPlayer.playbackState ==
                Player.STATE_IDLE ||
            exoPlayer.playbackState ==
                Player.STATE_ENDED
        ) {

            currentChannel?.let {

                playChannel(
                    it
                )

                return
            }
        }

        exoPlayer.playWhenReady =
            true

        exoPlayer.play()

        updatePlayPauseButton(
            true
        )
    }


    /* =========================================================
       SEARCH
       ========================================================= */

    private fun setupSearch() {

        searchButton.setOnClickListener {

            if (
                searchBox.visibility ==
                    View.GONE
            ) {

                searchBox.visibility =
                    View.VISIBLE

                searchBox.requestFocus()

            } else {

                searchBox.visibility =
                    View.GONE

                searchBox.text.clear()
            }
        }


        searchBox.addTextChangedListener(
            object : android.text.TextWatcher {

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }


                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) {

                    val query =
                        s?.toString()
                            ?.trim()
                            ?.lowercase()
                            ?: ""

                    val result =
                        if (query.isEmpty()) {

                            channels

                        } else {

                            channels.filter {

                                it.name
                                    .lowercase()
                                    .contains(
                                        query
                                    )
                            }
                        }

                    channelAdapter.updateList(
                        result
                    )

                    channelRecycler.post {

                        updateRecyclerHeight()
                    }
                }


                override fun afterTextChanged(
                    s: android.text.Editable?
                ) {
                }
            }
        )
    }


    /* =========================================================
       FULLSCREEN BUTTON
       ========================================================= */

    private fun setupFullscreenButton() {

        fullscreenButton.setOnClickListener {

            if (isFullscreen) {

                exitFullscreen()

            } else {

                enterFullscreen()
            }
        }
    }


    /* =========================================================
       ENTER FULLSCREEN
       ========================================================= */

    private fun enterFullscreen() {

        if (isFullscreen) {
            return
        }

        isFullscreen =
            true

        searchWasVisible =
            searchBox.visibility ==
                View.VISIBLE

        /*
         * Fullscreen শুরু করার আগে ScrollView একদম উপরে।
         */
        mainScrollView.scrollTo(
            0,
            0
        )

        /*
         * Fullscreen অবস্থায় ScrollView-এর scrollbar
         * দেখানো হবে না।
         */
        mainScrollView.isVerticalScrollBarEnabled =
            false

        mainScrollView.overScrollMode =
            View.OVER_SCROLL_NEVER

        /*
         * Landscape করা হচ্ছে।
         */
        requestedOrientation =
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        /*
         * Player ছাড়া বাকি UI hide।
         */
        headerLayout.visibility =
            View.GONE

        searchBox.visibility =
            View.GONE

        currentChannelName.visibility =
            View.GONE

        channelTitle.visibility =
            View.GONE

        channelRecycler.visibility =
            View.GONE

        footerLayout.visibility =
            View.GONE

        bottomNavigation.visibility =
            View.GONE

        /*
         * System bars hide।
         */
        hideSystemBars()

        playerControls.visibility =
            View.VISIBLE

        handler.removeCallbacks(
            hideControlsRunnable
        )

        /*
         * Orientation পরিবর্তনের পর actual viewport পাওয়া গেলে
         * player-এর height সেট হবে।
         */
        window.decorView.post {

            if (isFullscreen) {

                applyFullscreenPlayerSize()
            }
        }

        window.decorView.postDelayed({

            if (isFullscreen) {

                applyFullscreenPlayerSize()
            }

        }, 250)
    }


    /* =========================================================
       APPLY FULLSCREEN PLAYER SIZE
       ========================================================= */

    private fun applyFullscreenPlayerSize() {

        if (!isFullscreen) {
            return
        }

        /*
         * displayMetrics.heightPixels ব্যবহার করা হচ্ছে না।
         *
         * ScrollView-এর actual visible height ব্যবহার করা হচ্ছে।
         * এতে player নিজের viewport-এর বাইরে বড় হয়ে ScrollView
         * তৈরি করবে না।
         */
        val viewportWidth =
            mainScrollView.width

        val viewportHeight =
            mainScrollView.height

        /*
         * Layout এখনো measure না হলে আবার চেষ্টা।
         */
        if (
            viewportWidth <= 0 ||
            viewportHeight <= 0
        ) {

            window.decorView.post {

                if (isFullscreen) {

                    applyFullscreenPlayerSize()
                }
            }

            return
        }

        val params =
            playerContainer.layoutParams

        params.width =
            viewportWidth

        params.height =
            viewportHeight

        playerContainer.layoutParams =
            params

        /*
         * Fullscreen-এ ScrollView সবসময় top position-এ।
         */
        mainScrollView.scrollTo(
            0,
            0
        )

        /*
         * Layout update করানো।
         */
        playerContainer.requestLayout()
    }


    /* =========================================================
       EXIT FULLSCREEN
       ========================================================= */

    private fun exitFullscreen() {

        if (!isFullscreen) {
            return
        }

        /*
         * আগে fullscreen state বন্ধ।
         */
        isFullscreen =
            false

        handler.removeCallbacks(
            hideControlsRunnable
        )

        /*
         * Portrait-এ ফেরত যাওয়া।
         */
        requestedOrientation =
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        /*
         * System bars দেখানো।
         */
        showSystemBars()

        /*
         * এখনই player-এর height পরিবর্তন করছি না।
         *
         * কারণ orientation পরিবর্তনের মাঝখানে layout resize করলে
         * app ছোট/স্ক্রল হওয়ার সমস্যা হতে পারে।
         *
         * Portrait configuration আসার পর restoreNormalLayout()
         * সবকিছু একসাথে restore করবে।
         */
        window.decorView.postDelayed({

            if (!isFullscreen) {

                restoreNormalLayout()
            }

        }, 250)
    }


    /* =========================================================
       RESTORE NORMAL LAYOUT
       ========================================================= */

    private fun restoreNormalLayout() {

        if (isFullscreen) {
            return
        }

        val density =
            resources
                .displayMetrics
                .density

        /*
         * Player আবার 220dp।
         */
        val params =
            playerContainer.layoutParams

        params.width =
            ViewGroup.LayoutParams.MATCH_PARENT

        params.height =
            (
                normalPlayerHeight *
                    density
                ).toInt()

        playerContainer.layoutParams =
            params

        /*
         * সব UI আবার visible।
         */
        headerLayout.visibility =
            View.VISIBLE

        currentChannelName.visibility =
            View.VISIBLE

        channelTitle.visibility =
            View.VISIBLE

        channelRecycler.visibility =
            View.VISIBLE

        footerLayout.visibility =
            View.VISIBLE

        bottomNavigation.visibility =
            View.VISIBLE

        searchBox.visibility =
            if (searchWasVisible) {

                View.VISIBLE

            } else {

                View.GONE
            }

        /*
         * Normal mode-এ ScrollView আবার স্বাভাবিক।
         */
        mainScrollView.isVerticalScrollBarEnabled =
            true

        mainScrollView.overScrollMode =
            View.OVER_SCROLL_NEVER

        mainScrollView.scrollTo(
            0,
            0
        )

        /*
         * Channel grid-এর height পুনরায় ঠিক করা।
         */
        mainScrollView.post {

            updateRecyclerHeight()

            mainScrollView.scrollTo(
                0,
                0
            )
        }

        /*
         * Bottom navigation-এর system inset আবার apply।
         */
        ViewCompat.requestApplyInsets(
            bottomNavigation
        )

        playerControls.visibility =
            View.VISIBLE

        showControlsTemporarily()

        playerContainer.requestLayout()
    }


    /* =========================================================
       SYSTEM BARS
       ========================================================= */

    private fun hideSystemBars() {

        /*
         * গুরুত্বপূর্ণ:
         *
         * এখানে setDecorFitsSystemWindows(false) ব্যবহার করা হয়নি।
         *
         * এতে Root / ScrollView-এর layout mode বদলে যায় না।
         * শুধু system bars hide হবে।
         */
        val controller =
            WindowCompat.getInsetsController(
                window,
                window.decorView
            )

        controller.hide(
            WindowInsetsCompat.Type.systemBars()
        )

        controller.systemBarsBehavior =
            WindowInsetsControllerCompat
                .BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }


    private fun showSystemBars() {

        /*
         * এখানে setDecorFitsSystemWindows(true) ব্যবহার করা হয়নি।
         *
         * কারণ আমরা Activity-এর edge-to-edge layout mode
         * fullscreen-এর সময় পরিবর্তন করছি না।
         */
        val controller =
            WindowCompat.getInsetsController(
                window,
                window.decorView
            )

        controller.show(
            WindowInsetsCompat.Type.systemBars()
        )

        ViewCompat.requestApplyInsets(
            bottomNavigation
        )
    }


    /* =========================================================
       CONFIGURATION
       ========================================================= */

    override fun onConfigurationChanged(
        newConfig: Configuration
    ) {

        super.onConfigurationChanged(
            newConfig
        )

        window.decorView.post {

            if (
                isFullscreen &&
                newConfig.orientation ==
                    Configuration.ORIENTATION_LANDSCAPE
            ) {

                /*
                 * Landscape fullscreen।
                 */
                hideSystemBars()

                mainScrollView.isVerticalScrollBarEnabled =
                    false

                mainScrollView.overScrollMode =
                    View.OVER_SCROLL_NEVER

                mainScrollView.scrollTo(
                    0,
                    0
                )

                applyFullscreenPlayerSize()

            } else if (
                !isFullscreen &&
                newConfig.orientation ==
                    Configuration.ORIENTATION_PORTRAIT
            ) {

                /*
                 * Portrait normal mode।
                 */
                showSystemBars()

                restoreNormalLayout()
            }
        }
    }


    /* =========================================================
       BOTTOM MENU
       ========================================================= */

    private fun setupBottomMenu() {

        findViewById<View>(
            R.id.menu_home
        ).setOnClickListener {

            if (isFullscreen) {
                exitFullscreen()
                return@setOnClickListener
            }

            mainScrollView.post {

                mainScrollView.smoothScrollTo(
                    0,
                    0
                )
            }
        }


        findViewById<View>(
            R.id.menu_notification
        ).setOnClickListener {

            Toast.makeText(
                this,
                "কোনো নতুন Notification নেই",
                Toast.LENGTH_SHORT
            ).show()
        }


        findViewById<View>(
            R.id.menu_update
        ).setOnClickListener {

            openUpdateScreen()
        }


        findViewById<View>(
            R.id.menu_channel
        ).setOnClickListener {

            if (isFullscreen) {
                exitFullscreen()
                return@setOnClickListener
            }

            channelRecycler.post {

                mainScrollView.smoothScrollTo(
                    0,
                    channelRecycler.top
                )
            }
        }
    }


    /* =========================================================
       SOCIAL
       ========================================================= */

    private fun setupSocialLinks() {

        footerFacebook.setOnClickListener {

            openUrl(
                "https://www.facebook.com/royalcyber.7r"
            )
        }


        footerYoutube.setOnClickListener {

            openUrl(
                "https://www.youtube.com/@rhmultimedia5712"
            )
        }


        footerInstagram.setOnClickListener {

            openUrl(
                "https://www.instagram.com/crimeworld06266"
            )
        }


        footerTiktok.setOnClickListener {

            Toast.makeText(
                this,
                "TikTok link এখনো সেট করা হয়নি",
                Toast.LENGTH_SHORT
            ).show()
        }
    }


    private fun openUrl(
        url: String
    ) {

        try {

            val intent =
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(url)
                )

            startActivity(
                intent
            )

        } catch (
            _: ActivityNotFoundException
        ) {

            Toast.makeText(
                this,
                "Link খোলা যাচ্ছে না",
                Toast.LENGTH_SHORT
            ).show()
        }
    }


    /* =========================================================
       RESUME
       ========================================================= */

    override fun onResume() {

        super.onResume()

        if (isFullscreen) {

            hideSystemBars()

            window.decorView.post {

                if (isFullscreen) {

                    applyFullscreenPlayerSize()
                }
            }

        } else {

            ViewCompat.requestApplyInsets(
                bottomNavigation
            )
        }

        player?.let {

            if (
                it.playbackState ==
                    Player.STATE_IDLE ||
                it.playbackState ==
                    Player.STATE_ENDED
            ) {

                resumePlayback()

            } else {

                it.play()

                updatePlayPauseButton(
                    true
                )
            }
        }
    }


    /* =========================================================
       PAUSE
       ========================================================= */

    override fun onPause() {

        player?.pause()

        super.onPause()
    }


    /* =========================================================
       DESTROY
       ========================================================= */

    override fun onDestroy() {

        handler.removeCallbacksAndMessages(
            null
        )

        player?.release()

        player = null

        if (
            ::channelAdapter.isInitialized
        ) {

            channelAdapter.shutdown()
        }

        super.onDestroy()
    }
}
