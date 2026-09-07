package com.dj.insulink.shared.feature.auth.data.remote

// Iz GoogleService-Info.plist - iOS app je registrovana u Firebase konzoli (isti insulink-e8caa
// projekat) SAMO da bi se dobio ovaj OAuth klijent, ne koristimo GitLive/Firebase iOS SDK.
// Javni identifikatori (bezbedno za commit) - Google-ov OAuth model se oslanja na redirect-URI
// restrikciju vezanu za bundle ID (com.dj.insulink.ios), ne na tajnost client ID-a; isti razlog
// zašto se ovi stringovi vide i u samom URL scheme-u u Info.plist.
internal const val GOOGLE_IOS_CLIENT_ID =
    "846271429991-hr3eu5bnqf995kl361be02e1huhcbj3q.apps.googleusercontent.com"
internal const val GOOGLE_IOS_REVERSED_CLIENT_ID =
    "com.googleusercontent.apps.846271429991-hr3eu5bnqf995kl361be02e1huhcbj3q"
