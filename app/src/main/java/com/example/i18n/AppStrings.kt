package com.example.i18n

import com.example.domain.AppLanguage

data class FaqItem(
    val question: String,
    val answer: String
)

data class FeatureItem(
    val title: String,
    val description: String
)

data class StepItem(
    val number: String,
    val title: String,
    val description: String
)

data class LocalizedStrings(
    val language: AppLanguage,
    // Navigation
    val brandName: String,
    val brandTagline: String,
    val navHowItWorks: String,
    val navFeatures: String,
    val navFaq: String,
    val navSecuritySpec: String,
    val themeLightLabel: String,
    val themeDarkLabel: String,
    // Hero & Main Download Card
    val heroPlatformBadge: String,
    val heroHeadline: String,
    val heroSubtitle: String,
    val inputLabel: String,
    val inputPlaceholder: String,
    val pasteButton: String,
    val clearButton: String,
    val extractButton: String,
    val supportedFormatsHint: String,
    val sampleFacebookUrlsLabel: String,
    val livePipelineDiagnosticButton: String,
    val rateLimitQuotaLabel: String,
    // Validation & Error States
    val stateEmptyTitle: String,
    val stateEmptyMessage: String,
    val stateInvalidUrlTitle: String,
    val stateInvalidUrlMessage: String,
    val stateUnsupportedProtocolTitle: String,
    val stateUnsupportedProtocolMessage: String,
    val stateSsrfBlockedTitle: String,
    val stateSsrfBlockedMessage: String,
    val stateUnsupportedPlatformTitle: String,
    val stateUnsupportedPlatformMessage: String,
    val stateUnsupportedFbPathTitle: String,
    val stateUnsupportedFbPathMessage: String,
    val stateRateLimitedTitle: String,
    val stateRateLimitedMessage: String,
    val stateExtractingTitle: String,
    val stateExtractingSubtitle: String,
    // Video Preview & Quality Selection
    val stateMetadataReadyBadge: String,
    val diagnosticStreamNotice: String,
    val videoDurationLabel: String,
    val videoAuthorLabel: String,
    val extractionStrategyLabel: String,
    val selectQualityLabel: String,
    val sizeUnknownLabel: String,
    val downloadSelectedButton: String,
    val statePreparingDownload: String,
    val stateDownloadingTitle: String,
    val stateDownloadingIndeterminate: String,
    val stateDownloadCompletedTitle: String,
    val stateDownloadCompletedSubtitle: String,
    val savedFilePathLabel: String,
    val verifiedFileSizeLabel: String,
    val sha256IntegrityLabel: String,
    val shareOrOpenVideoButton: String,
    val downloadAnotherButton: String,
    // Extraction Failure States
    val stateExtractionFailedTitle: String,
    val statePrivateOrLoginWallGuidance: String,
    val stateNetworkErrorGuidance: String,
    val retryButton: String,
    // How It Works
    val howItWorksTitle: String,
    val howItWorksSubtitle: String,
    val steps: List<StepItem>,
    // Features
    val featuresTitle: String,
    val featuresSubtitle: String,
    val features: List<FeatureItem>,
    // FAQ
    val faqTitle: String,
    val faqSubtitle: String,
    val faqs: List<FaqItem>,
    // Legal, Security & Footer
    val securityArchitectureTitle: String,
    val securityArchitectureBullets: List<String>,
    val privacyPolicyTitle: String,
    val privacyPolicyBody: String,
    val termsOfUseTitle: String,
    val termsOfUseBody: String,
    val footerDisclaimer: String,
    val footerContactPlaceholder: String,
    val closeButton: String
)

object AppStringsRepository {

    fun forLanguage(language: AppLanguage): LocalizedStrings = when (language) {
        AppLanguage.EN -> ENGLISH
        AppLanguage.AR -> ARABIC
        AppLanguage.FR -> FRENCH
    }

    private val ENGLISH = LocalizedStrings(
        language = AppLanguage.EN,
        brandName = "VeloStream",
        brandTagline = "Precision Facebook Video Extractor",
        navHowItWorks = "How It Works",
        navFeatures = "Features",
        navFaq = "FAQ",
        navSecuritySpec = "Security & API",
        themeLightLabel = "Light Mode",
        themeDarkLabel = "Dark Mode",
        heroPlatformBadge = "Currently Supported Platform: Facebook Public Videos (v1.0)",
        heroHeadline = "Download Public Facebook Videos in Original MP4 Quality",
        heroSubtitle = "Paste a publicly accessible Facebook Watch, Reel, or video permalink to inspect real stream metadata, choose HD or SD resolution, and stream the MP4 directly to your device.",
        inputLabel = "Facebook Video URL",
        inputPlaceholder = "https://www.facebook.com/watch/?v=... or https://fb.watch/...",
        pasteButton = "Paste",
        clearButton = "Clear",
        extractButton = "Retrieve Video Info",
        supportedFormatsHint = "Supported: facebook.com/watch/?v=..., fb.watch/..., facebook.com/share/v/..., facebook.com/reel/..., and public video permalinks.",
        sampleFacebookUrlsLabel = "Quick Format Examples:",
        livePipelineDiagnosticButton = "Verify Live MP4 Download Pipeline (Public Stream)",
        rateLimitQuotaLabel = "Security Guard: SSRF Protected • Rate Limit Active",
        stateEmptyTitle = "URL Required",
        stateEmptyMessage = "Please paste a valid Facebook video link into the input field before submitting.",
        stateInvalidUrlTitle = "Invalid URL Format",
        stateInvalidUrlMessage = "The entered text is not a valid web URL. Make sure it starts with https://www.facebook.com/ or https://fb.watch/.",
        stateUnsupportedProtocolTitle = "Blocked Protocol",
        stateUnsupportedProtocolMessage = "Only HTTPS and HTTP links are permitted. Local files, scripts, and custom schemes are blocked.",
        stateSsrfBlockedTitle = "Security Policy Blocked Request (SSRF Protection)",
        stateSsrfBlockedMessage = "Requests to localhost, private internal networks (RFC1918), raw IP literals, embedded credentials, and cloud metadata endpoints are strictly forbidden.",
        stateUnsupportedPlatformTitle = "Platform Not Enabled in v1.0",
        stateUnsupportedPlatformMessage = "This version of VeloStream is built exclusively for Facebook video links. Our modular provider engine reserves TikTok and other platforms for future modules.",
        stateUnsupportedFbPathTitle = "Unrecognized Facebook Video Permalink",
        stateUnsupportedFbPathMessage = "This link points to Facebook, but does not match a video path (/watch, /videos/, /reel/, /share/v/, or fb.watch).",
        stateRateLimitedTitle = "Rate Limit Reached",
        stateRateLimitedMessage = "Too many extraction requests in a short window. Please wait a few seconds before trying again.",
        stateExtractingTitle = "Retrieving Real Video Metadata...",
        stateExtractingSubtitle = "Validating host security, querying Facebook Relay/Embed endpoints, and probing available MP4 streams.",
        stateMetadataReadyBadge = "Video Information Retrieved",
        diagnosticStreamNotice = "Live Pipeline Verification Stream — Demonstrates real HTTP MP4 byte streaming, SHA-256 verification, and file saving when Facebook restricts unauthenticated access.",
        videoDurationLabel = "Duration",
        videoAuthorLabel = "Publisher",
        extractionStrategyLabel = "Extractor Engine",
        selectQualityLabel = "Select Available Quality & Format",
        sizeUnknownLabel = "Stream size determined during download",
        downloadSelectedButton = "Download Selected MP4",
        statePreparingDownload = "Preparing secure binary stream and verifying MP4 headers...",
        stateDownloadingTitle = "Downloading Actual Video File...",
        stateDownloadingIndeterminate = "Streaming binary MP4 chunks (Content-Length not advertised by CDN)...",
        stateDownloadCompletedTitle = "Download Completed & Verified",
        stateDownloadCompletedSubtitle = "The binary MP4 stream was verified and saved to your device storage.",
        savedFilePathLabel = "Saved Location",
        verifiedFileSizeLabel = "Verified File Size",
        sha256IntegrityLabel = "SHA-256 Integrity",
        shareOrOpenVideoButton = "Open / Share MP4 File",
        downloadAnotherButton = "Process Another Link",
        stateExtractionFailedTitle = "Unable to Retrieve Video Stream",
        statePrivateOrLoginWallGuidance = "Why this happened: Facebook requires authentication, age verification, or private group access for this video, or blocked unauthenticated server extraction. VeloStream never bypasses login walls, private restrictions, or DRM. You can test another public video or click 'Verify Live MP4 Download Pipeline' below to test the real binary downloader.",
        stateNetworkErrorGuidance = "Check your internet connection or verify whether Facebook is reachable from your current network environment.",
        retryButton = "Try Again",
        howItWorksTitle = "How It Works",
        howItWorksSubtitle = "Three transparent steps from link validation to verified MP4 download.",
        steps = listOf(
            StepItem(
                number = "01",
                title = "Copy a Public Facebook Video Link",
                description = "Copy the link to any publicly accessible Facebook Watch video, Reel, or permalink that you are authorized to download."
            ),
            StepItem(
                number = "02",
                title = "Paste & Inspect Real Metadata",
                description = "Paste the URL into VeloStream. Our extractor validates the URL against SSRF rules and parses real progressive HD/SD streams."
            ),
            StepItem(
                number = "03",
                title = "Select Quality & Stream MP4",
                description = "Pick your preferred available resolution and stream the binary MP4 directly to your device with SHA-256 integrity verification."
            )
        ),
        featuresTitle = "Engineered for Reliability & Security",
        featuresSubtitle = "Only real, implemented capabilities — zero fake progress bars or simulated downloads.",
        features = listOf(
            FeatureItem(
                title = "Real Relay & OpenGraph Extraction",
                description = "Parses actual browser_native_hd_url, browser_native_sd_url, playable_url, and OpenGraph streams directly from public Facebook responses."
            ),
            FeatureItem(
                title = "No Registration Required",
                description = "Access the complete public video extraction workflow immediately without creating an account or storing personal credentials."
            ),
            FeatureItem(
                title = "HD & SD Quality Selection",
                description = "Displays only the real quality options discovered for each video and probes Content-Length for accurate file sizes."
            ),
            FeatureItem(
                title = "Strict SSRF & Redirect Guard",
                description = "Blocks localhost, private RFC1918 subnets, cloud metadata IPs (169.254.169.254), non-HTTP schemes, and unsafe redirect hops."
            ),
            FeatureItem(
                title = "Chunked Binary Streaming",
                description = "Streams video data in 32 KB buffers without loading large files into RAM, verifying MP4 headers and computing SHA-256 checksums."
            ),
            FeatureItem(
                title = "Complete EN / AR (RTL) / FR Localization",
                description = "Instant language switching across English, right-to-left Arabic, and French with persisted user preference."
            ),
            FeatureItem(
                title = "Adaptive Light & Dark Modes",
                description = "High-contrast light and dark color schemes that respect system defaults and persist your manual selection."
            ),
            FeatureItem(
                title = "Modular Provider Architecture",
                description = "Built around a clean VideoProvider interface scoped to Facebook in v1.0 and ready for plug-and-play TikTok support."
            )
        ),
        faqTitle = "Frequently Asked Questions",
        faqSubtitle = "Honest answers about supported links, technical scope, privacy, and copyright.",
        faqs = listOf(
            FaqItem(
                question = "How do I download a supported Facebook video?",
                answer = "Copy the public link from Facebook (Share -> Copy Link), paste it into the URL box above, tap 'Retrieve Video Info', choose an available HD or SD quality, and tap 'Download Selected MP4'."
            ),
            FaqItem(
                question = "Which Facebook URL formats are supported?",
                answer = "We validate and normalize facebook.com/watch/?v=..., www.facebook.com/watch/?v=..., fb.watch/..., facebook.com/share/v/..., facebook.com/reel/..., and standard /<page>/videos/<id> permalinks."
            ),
            FaqItem(
                question = "Why can some Facebook videos not be retrieved?",
                answer = "Private videos, friends-only posts, age-restricted content, DRM-protected streams, or videos behind Facebook's login wall cannot be accessed without authentication. We do not bypass access controls or technical protections."
            ),
            FaqItem(
                question = "Which quality options are available?",
                answer = "Available qualities depend entirely on what Facebook exposes for that video—typically Progressive HD (720p/1080p MP4) and Progressive SD (360p/480p MP4). We never display fake quality options."
            ),
            FaqItem(
                question = "Does VeloStream store submitted URLs or downloaded files on external servers?",
                answer = "No external tracking database is used. Temporary .part files created during active streaming are automatically cleaned up if interrupted, and completed MP4 files are stored locally in your device's Downloads folder."
            ),
            FaqItem(
                question = "How should I respect copyright and content ownership?",
                answer = "Only download videos that you own, have explicit permission from the copyright holder to download, or that are distributed under a permissive license."
            )
        ),
        securityArchitectureTitle = "Security, API Contract & Deployment Specification",
        securityArchitectureBullets = listOf(
            "Provider Scope: FacebookVideoProvider active in v1.0; TikTokVideoProviderStub registered in ProviderRegistry for modular expansion.",
            "SSRF Defense: Rejects localhost, 127.0.0.0/8, 10.0.0.0/8, 172.16.0.0/12, 192.168.0.0/16, 169.254.169.254, IPv6 literals, and validates every HTTP 3xx redirect hop.",
            "Stream Verification: Rejects text/html responses masquerading as media, enforces a 500 MB ceiling, streams in 32 KB chunks, and computes a SHA-256 digest.",
            "Rate Limiting: Sliding-window limiter (8 requests / 60s) protects extraction endpoints against automated abuse.",
            "Temp File Lifecycle: Active downloads write to timestamped .part files and expire stale partials older than 30 minutes."
        ),
        privacyPolicyTitle = "Privacy Policy",
        privacyPolicyBody = "VeloStream processes the Facebook URL you submit solely to request public video metadata and stream the selected MP4 file. Language and theme preferences are stored locally on your device. No personal account credentials or cookies are collected or transmitted to third parties.",
        termsOfUseTitle = "Terms of Use",
        termsOfUseBody = "Users are solely responsible for ensuring they have the legal right to download any video submitted through VeloStream. Do not use this tool to infringe intellectual property rights or circumvent access controls.",
        footerDisclaimer = "Independent Tool Notice: VeloStream is an independent software utility and is NOT affiliated with, endorsed by, or sponsored by Meta Platforms, Inc. or Facebook.",
        footerContactPlaceholder = "Support & Security Reporting: contact@velostream-example.invalid (Placeholder)",
        closeButton = "Close"
    )

    private val ARABIC = LocalizedStrings(
        language = AppLanguage.AR,
        brandName = "فيلو ستريم",
        brandTagline = "أداة احترافية لاستخراج وتحميل فيديوهات فيسبوك",
        navHowItWorks = "طريقة العمل",
        navFeatures = "المميزات",
        navFaq = "الأسئلة الشائعة",
        navSecuritySpec = "الأمان والمعمارية",
        themeLightLabel = "الوضع الفاتح",
        themeDarkLabel = "الوضع الداكن",
        heroPlatformBadge = "المنصة المدعومة حالياً: فيديوهات فيسبوك العامة (الإصدار 1.0)",
        heroHeadline = "حمّل فيديوهات فيسبوك العامة بجودة MP4 الأصلية",
        heroSubtitle = "الصق رابط فيديو فيسبوك عام (Watch أو Reel أو رابط مباشر) لاستخراج البيانات الحقيقية للفيديو، واختيار الجودة المتاحة HD أو SD، وتنزيل ملف MP4 الفعلي مباشرة.",
        inputLabel = "رابط فيديو فيسبوك",
        inputPlaceholder = "https://www.facebook.com/watch/?v=... أو https://fb.watch/...",
        pasteButton = "لصق",
        clearButton = "مسح",
        extractButton = "استخراج معلومات الفيديو",
        supportedFormatsHint = "الروابط المدعومة: facebook.com/watch/?v=... و fb.watch/... و facebook.com/share/v/... و facebook.com/reel/...",
        sampleFacebookUrlsLabel = "أمثلة لصيغ الروابط:",
        livePipelineDiagnosticButton = "اختبار خط التحميل المباشر (فيديو عام للتحقق)",
        rateLimitQuotaLabel = "حماية أمنية: فحص SSRF مفعّل • تحديد معدل الطلبات نشط",
        stateEmptyTitle = "الرابط مطلوب",
        stateEmptyMessage = "يرجى لصق رابط فيديو فيسبوك صالح في حقل الإدخال قبل المتابعة.",
        stateInvalidUrlTitle = "صيغة الرابط غير صالحة",
        stateInvalidUrlMessage = "النص المدخل ليس رابط ويب صالحاً. تأكد من أنه يبدأ بـ https://www.facebook.com/ أو https://fb.watch/.",
        stateUnsupportedProtocolTitle = "بروتوكول غير مسموح",
        stateUnsupportedProtocolMessage = "يُسمح فقط بروابط HTTPS و HTTP. تم حظر البروتوكولات المحلية والنصوص البرمجية.",
        stateSsrfBlockedTitle = "تم حظر الطلب بواسطة نظام الحماية (SSRF)",
        stateSsrfBlockedMessage = "يُمنع منعاً باتاً إرسال طلبات إلى الخادم المحلي (localhost) أو الشبكات الداخلية الخاصة أو عناوين IP المباشرة أو بيانات تعريف السحابة.",
        stateUnsupportedPlatformTitle = "المنصة غير مفعّلة في الإصدار 1.0",
        stateUnsupportedPlatformMessage = "هذا الإصدار مخصص حصرياً لروابط فيديو فيسبوك. تم تصميم النظام بشكل معياري لإضافة دعم تيك توك (TikTok) لاحقاً دون إعادة كتابة التطبيق.",
        stateUnsupportedFbPathTitle = "رابط فيسبوك غير مخصص للفيديو",
        stateUnsupportedFbPathMessage = "هذا الرابط ينتمي إلى فيسبوك ولكنه لا يطابق مسار فيديو معروف (/watch أو /videos/ أو /reel/ أو /share/v/ أو fb.watch).",
        stateRateLimitedTitle = "تم تجاوز الحد المسموح للطلبات",
        stateRateLimitedMessage = "تم إرسال عدد كبير من الطلبات خلال فترة قصيرة. يرجى الانتظار بضع ثوانٍ ثم المحاولة مرة أخرى.",
        stateExtractingTitle = "جارٍ استخراج بيانات الفيديو الحقيقية...",
        stateExtractingSubtitle = "يتم التحقق من أمان النطاق، وفحص استجابة فيسبوك، واستكشاف مسارات البث المتاحة بصيغة MP4.",
        stateMetadataReadyBadge = "تم استرجاع بيانات الفيديو بنجاح",
        diagnosticStreamNotice = "بث تحقّق حي — يثبت عمل تدفق البيانات الفعلي بصيغة MP4 والتحقق من البصمة الرقمية SHA-256 وحفظ الملف على الجهاز.",
        videoDurationLabel = "المدة",
        videoAuthorLabel = "الناشر",
        extractionStrategyLabel = "محرك الاستخراج",
        selectQualityLabel = "اختر الجودة والصيغة المتاحة",
        sizeUnknownLabel = "يتم تحديد الحجم الفعلي أثناء التدفق",
        downloadSelectedButton = "تحميل ملف MP4 المختار",
        statePreparingDownload = "جارٍ تجهيز اتصال التحميل الآمن والتحقق من ترويسة ملف MP4...",
        stateDownloadingTitle = "جارٍ تنزيل ملف الفيديو الفعلي...",
        stateDownloadingIndeterminate = "جارٍ تدفق البيانات الثنائية لملف MP4...",
        stateDownloadCompletedTitle = "اكتمل التحميل وتم التحقق من سلامة الملف",
        stateDownloadCompletedSubtitle = "تم تنزيل ملف MP4 الفعلي والتحقق من صحته وحفظه في وحدة تخزين جهازك.",
        savedFilePathLabel = "مسار الحفظ",
        verifiedFileSizeLabel = "حجم الملف المؤكد",
        sha256IntegrityLabel = "بصمة التحقق SHA-256",
        shareOrOpenVideoButton = "فتح / مشاركة ملف MP4",
        downloadAnotherButton = "تحميل فيديو آخر",
        stateExtractionFailedTitle = "تعذر استخراج دفق الفيديو",
        statePrivateOrLoginWallGuidance = "سبب ذلك: قد يتطلب هذا الفيديو تسجيل الدخول إلى فيسبوك، أو أنه خاص، أو مقيد بالعمر، أو أن فيسبوك حجب الاستخراج غير المصادق عليه. نحن لا نتجاوز قيود الخصوصية أو جدران تسجيل الدخول أبداً. يمكنك تجربة رابط عام آخر أو النقر على زر 'اختبار خط التحميل المباشر' أدناه لتجربة تنزيل ملف MP4 فعلي.",
        stateNetworkErrorGuidance = "يرجى التحقق من اتصالك بالإنترنت أو التأكد من إمكانية الوصول إلى خوادم فيسبوك من شبكتك الحالية.",
        retryButton = "إعادة المحاولة",
        howItWorksTitle = "كيف يعمل التطبيق؟",
        howItWorksSubtitle = "ثلاث خطوات واضحة من التحقق من الرابط حتى حفظ ملف MP4 الحقيقي.",
        steps = listOf(
            StepItem(
                number = "٠١",
                title = "انسخ رابط فيديو فيسبوك عام",
                description = "انسخ رابط أي فيديو عام على فيسبوك (Watch أو Reel أو منشور فيديو) تملك صلاحية تنزيله."
            ),
            StepItem(
                number = "٠٢",
                title = "الصق الرابط واستعرض البيانات الحقيقية",
                description = "يتحقق النظام من أمان الرابط ويستخرج العناوين، الصورة المصغرة، والمدد والجودات المتوفرة فعلياً."
            ),
            StepItem(
                number = "٠٣",
                title = "اختر الجودة وحمّل ملف MP4",
                description = "حدد الدقة المناسبة (HD أو SD) ليتم تنزيل الملف الثنائي مباشرة مع فحص سلامة البيانات SHA-256."
            )
        ),
        featuresTitle = "مصمم للأمان والأداء الحقيقي",
        featuresSubtitle = "جميع المميزات المعروضة منفذة فعلياً بدون أي محاكاة أو أشرطة تقدم وهمية.",
        features = listOf(
            FeatureItem(
                title = "استخراج حقيقي لبيانات فيسبوك",
                description = "تحليل مباشر لروابط البث الأصلية browser_native_hd_url و browser_native_sd_url وبيانات OpenGraph."
            ),
            FeatureItem(
                title = "بدون تسجيل حساب إجباري",
                description = "استخدم أداة التحميل الأساسية مباشرة دون الحاجة إلى إنشاء حساب أو إدخال بيانات شخصية."
            ),
            FeatureItem(
                title = "اختيار الجودات المتوفرة فعلياً",
                description = "عرض الجودات الحقيقية فقط مع فحص حجم الملف (Content-Length) قبل بدء التنزيل."
            ),
            FeatureItem(
                title = "حماية صارمة ضد هجمات SSRF",
                description = "حظر تلقائي للشبكات المحلية والعناوين الداخلية وإعادة التوجيه غير الآمنة."
            ),
            FeatureItem(
                title = "تدفق مباشر للبيانات (Streaming)",
                description = "تنزيل الفيديو عبر أجزاء صغيرة دون استهلاك الذاكرة العشوائية مع رفض صفحات HTML الخاطئة."
            ),
            FeatureItem(
                title = "دعم كامل للعربية (RTL) والإنجليزية والفرنسية",
                description = "تبديل فوري للغة مع تخطيط أصيل من اليمين إلى اليسار للغة العربية وحفظ التفضيل تلقائياً."
            ),
            FeatureItem(
                title = "الوضع الفاتح والداكن",
                description = "نظام ألوان متكامل يدعم الوضعين الفاتح والداكن بتباين مريح للعين."
            ),
            FeatureItem(
                title = "بنية معيارية قابلة للتوسع",
                description = "معمارية تفصل مزود فيسبوك في وحدة مستقلة مما يتيح إضافة تيك توك مستقبلاً بسهولة."
            )
        ),
        faqTitle = "الأسئلة الشائعة",
        faqSubtitle = "إجابات شفافة حول الروابط المدعومة، القيود التقنية، والخصوصية.",
        faqs = listOf(
            FaqItem(
                question = "كيف يمكنني تحميل فيديو مدعوم من فيسبوك؟",
                answer = "انسخ رابط الفيديو العام من فيسبوك، الصقه في الحقل المخصص أعلاه، اضغط على 'استخراج معلومات الفيديو'، ثم اختر الجودة المتاحة واضغط على 'تحميل ملف MP4 المختار'."
            ),
            FaqItem(
                question = "ما هي صيغ روابط فيسبوك المدعومة؟",
                answer = "ندعم الروابط العامة بصيغ facebook.com/watch/?v=... و fb.watch/... و facebook.com/share/v/... و facebook.com/reel/... والروابط الدائمة للفيديوهات العامة."
            ),
            FaqItem(
                question = "لماذا يتعذر استخراج بعض الفيديوهات؟",
                answer = "الفيديوهات الخاصة، أو المخصصة للأصدقاء فقط، أو المقيدة بالعمر، أو التي يفرض عليها فيسبوك جدار تسجيل دخول لا يمكن الوصول إليها بدون مصادقة. نحن لا نقوم بكسر الحماية أو تجاوز صلاحيات الوصول."
            ),
            FaqItem(
                question = "ما هي خيارات الجودة المتاحة؟",
                answer = "تعتمد الجودات على ما يوفره فيسبوك لكل فيديو، وعادةً تشمل الجودة العالية HD والجودة القياسية SD بصيغة MP4."
            ),
            FaqItem(
                question = "هل يتم تخزين الروابط أو ملفات الفيديو؟",
                answer = "لا نقوم بتخزين سجلات الروابط على خوادم خارجية. يتم حفظ ملف الفيديو النهائي محلياً على جهازك، وتُحذف الملفات المؤقتة تلقائياً."
            ),
            FaqItem(
                question = "كيف أحترم حقوق الملكية الفكرية؟",
                answer = "يرجى تنزيل الفيديوهات التي تمتلك حقوقها أو لديك إذن صريح من صاحبها أو المتاحة بموجب ترخيص يسمح بإعادة الاستخدام فقط."
            )
        ),
        securityArchitectureTitle = "مواصفات الأمان ومعمارية النظام",
        securityArchitectureBullets = listOf(
            "نطاق المزودات: وحدة FacebookVideoProvider نشطة في الإصدار الأول مع جاهزية ProviderRegistry لإضافة TikTok.",
            "الحماية من SSRF: منع كامل لعناوين الشبكات الداخلية و localhost والتحقق من كل خطوة إعادة توجيه (Redirect).",
            "التحقق من الملفات: رفض أي استجابة HTML غير صالحة، وحساب البصمة الرقمية SHA-256 أثناء التدفق.",
            "تحديد معدل الطلبات: نظام حماية يمنع إساءة الاستخدام عبر تحديد عدد الطلبات في الدقيقة.",
            "إدارة الملفات المؤقتة: تنظيف تلقائي للملفات الجزئية المنتهية الصلاحية."
        ),
        privacyPolicyTitle = "سياسة الخصوصية",
        privacyPolicyBody = "يقوم تطبيق فيلو ستريم بمعالجة رابط فيسبوك الذي تدخله فقط لغرض استرجاع بيانات الفيديو العامة وتنزيل ملف MP4 المختار. يتم حفظ إعدادات اللغة والمظهر محلياً على جهازك دون جمع أي بيانات اعتماد شخصية.",
        termsOfUseTitle = "شروط الاستخدام",
        termsOfUseBody = "يتحمل المستخدم المسؤولية الكاملة عن التأكد من امتلاكه الحق القانوني لتنزيل أي محتوى. يُمنع استخدام الأداة لانتهاك حقوق النشر أو تجاوز ضوابط الوصول.",
        footerDisclaimer = "إخلاء مسؤولية: فيلو ستريم أداة مستقلة تماماً وليست تابعة لشركة Meta Platforms, Inc. أو فيسبوك ولا معتمدة منها.",
        footerContactPlaceholder = "التواصل والإبلاغ الأمني: contact@velostream-example.invalid (نموذج توضيحي)",
        closeButton = "إغلاق"
    )

    private val FRENCH = LocalizedStrings(
        language = AppLanguage.FR,
        brandName = "VeloStream",
        brandTagline = "Extracteur Vidéo Facebook Haute Précision",
        navHowItWorks = "Fonctionnement",
        navFeatures = "Fonctionnalités",
        navFaq = "FAQ",
        navSecuritySpec = "Sécurité & API",
        themeLightLabel = "Mode Clair",
        themeDarkLabel = "Mode Sombre",
        heroPlatformBadge = "Plateforme prise en charge : Vidéos publiques Facebook (v1.0)",
        heroHeadline = "Téléchargez vos vidéos publiques Facebook en qualité MP4 originale",
        heroSubtitle = "Collez un lien vidéo Facebook public (Watch, Reel ou permalien) pour extraire les métadonnées réelles, sélectionner la qualité HD ou SD disponible et télécharger le fichier MP4.",
        inputLabel = "URL de la vidéo Facebook",
        inputPlaceholder = "https://www.facebook.com/watch/?v=... ou https://fb.watch/...",
        pasteButton = "Coller",
        clearButton = "Effacer",
        extractButton = "Analyser la vidéo",
        supportedFormatsHint = "Formats pris en charge : facebook.com/watch/?v=..., fb.watch/..., facebook.com/share/v/..., facebook.com/reel/... et permaliens publics.",
        sampleFacebookUrlsLabel = "Exemples de formats :",
        livePipelineDiagnosticButton = "Tester le flux de téléchargement MP4 réel (Vidéo publique)",
        rateLimitQuotaLabel = "Sécurité active : Protection SSRF • Limitation de débit",
        stateEmptyTitle = "URL requise",
        stateEmptyMessage = "Veuillez coller un lien vidéo Facebook valide dans le champ de saisie avant de valider.",
        stateInvalidUrlTitle = "Format d'URL invalide",
        stateInvalidUrlMessage = "Le texte saisi n'est pas une adresse web valide. Vérifiez qu'elle commence par https://www.facebook.com/ ou https://fb.watch/.",
        stateUnsupportedProtocolTitle = "Protocole bloqué",
        stateUnsupportedProtocolMessage = "Seuls les protocoles HTTPS et HTTP sont autorisés. Les schémas locaux et scripts sont rejetés.",
        stateSsrfBlockedTitle = "Requête bloquée par la sécurité (Protection SSRF)",
        stateSsrfBlockedMessage = "Les requêtes vers localhost, les réseaux privés internes (RFC1918), les adresses IP brutes et les services de métadonnées cloud sont strictement interdites.",
        stateUnsupportedPlatformTitle = "Plateforme non activée dans la v1.0",
        stateUnsupportedPlatformMessage = "Cette première version est dédiée exclusivement à Facebook. Notre architecture modulaire permet d'ajouter TikTok ultérieurement sans réécrire l'application.",
        stateUnsupportedFbPathTitle = "Lien Facebook non reconnu comme vidéo",
        stateUnsupportedFbPathMessage = "Ce lien pointe vers Facebook mais ne correspond pas à un permalien vidéo (/watch, /videos/, /reel/, /share/v/ ou fb.watch).",
        stateRateLimitedTitle = "Limite de requêtes atteinte",
        stateRateLimitedMessage = "Trop de requêtes envoyées récemment. Veuillez patienter quelques secondes avant de réessayer.",
        stateExtractingTitle = "Extraction des métadonnées réelles en cours...",
        stateExtractingSubtitle = "Validation de sécurité, interrogation des points de terminaison Facebook et détection des flux MP4.",
        stateMetadataReadyBadge = "Informations vidéo récupérées",
        diagnosticStreamNotice = "Flux de vérification en direct — Démontre le téléchargement binaire MP4 réel, le contrôle SHA-256 et l'enregistrement local.",
        videoDurationLabel = "Durée",
        videoAuthorLabel = "Auteur",
        extractionStrategyLabel = "Moteur d'extraction",
        selectQualityLabel = "Choisissez la qualité et le format disponibles",
        sizeUnknownLabel = "Taille calculée pendant le téléchargement",
        downloadSelectedButton = "Télécharger le MP4 sélectionné",
        statePreparingDownload = "Préparation du flux binaire sécurisé et vérification des en-têtes MP4...",
        stateDownloadingTitle = "Téléchargement du fichier vidéo réel...",
        stateDownloadingIndeterminate = "Réception des blocs binaires MP4 en flux continu...",
        stateDownloadCompletedTitle = "Téléchargement terminé et vérifié",
        stateDownloadCompletedSubtitle = "Le fichier vidéo MP4 a été vérifié par empreinte SHA-256 et enregistré sur votre appareil.",
        savedFilePathLabel = "Emplacement du fichier",
        verifiedFileSizeLabel = "Taille réelle vérifiée",
        sha256IntegrityLabel = "Empreinte SHA-256",
        shareOrOpenVideoButton = "Ouvrir / Partager le fichier MP4",
        downloadAnotherButton = "Télécharger une autre vidéo",
        stateExtractionFailedTitle = "Impossible de récupérer le flux vidéo",
        statePrivateOrLoginWallGuidance = "Explication : Facebook exige une connexion, une vérification d'âge ou des droits d'accès privés pour cette vidéo, ou bloque l'extraction sans authentification. VeloStream ne contourne jamais les contrôles d'accès ni les DRM. Vous pouvez tester une autre vidéo publique ou cliquer sur « Tester le flux de téléchargement MP4 réel » ci-dessous.",
        stateNetworkErrorGuidance = "Vérifiez votre connexion réseau ou assurez-vous que Facebook est accessible depuis votre environnement actuel.",
        retryButton = "Réessayer",
        howItWorksTitle = "Comment ça marche",
        howItWorksSubtitle = "Trois étapes simples, de la validation du lien jusqu'au fichier MP4 vérifié.",
        steps = listOf(
            StepItem(
                number = "01",
                title = "Copiez un lien vidéo Facebook public",
                description = "Copiez l'URL d'une vidéo publique Facebook Watch, Reel ou permalien que vous êtes autorisé à télécharger."
            ),
            StepItem(
                number = "02",
                title = "Collez le lien et inspectez les métadonnées",
                description = "VeloStream valide la sécurité de l'URL et extrait le titre, la miniature, la durée et les résolutions réellement disponibles."
            ),
            StepItem(
                number = "03",
                title = "Sélectionnez la qualité et téléchargez",
                description = "Choisissez la qualité souhaitée (HD ou SD) et téléchargez le flux MP4 réel avec contrôle d'intégrité SHA-256."
            )
        ),
        featuresTitle = "Conçu pour la fiabilité et la transparence",
        featuresSubtitle = "Uniquement des fonctionnalités réellement implémentées — aucune simulation.",
        features = listOf(
            FeatureItem(
                title = "Extraction Relay & OpenGraph réelle",
                description = "Analyse les flux browser_native_hd_url, browser_native_sd_url et OpenGraph issus des réponses publiques de Facebook."
            ),
            FeatureItem(
                title = "Sans inscription obligatoire",
                description = "Utilisez le flux de téléchargement immédiatement sans créer de compte ni fournir d'identifiants."
            ),
            FeatureItem(
                title = "Sélection de qualité HD / SD",
                description = "Affiche uniquement les formats réellement détectés avec vérification de la taille du fichier."
            ),
            FeatureItem(
                title = "Protection SSRF et redirections sécurisées",
                description = "Bloque localhost, les sous-réseaux privés RFC1918, les IP de métadonnées cloud et valide chaque redirection."
            ),
            FeatureItem(
                title = "Streaming binaire par blocs",
                description = "Télécharge les vidéos par segments de 32 Ko avec vérification d'en-tête MP4 et calcul d'empreinte SHA-256."
            ),
            FeatureItem(
                title = "Interface trilingue EN / AR (RTL) / FR",
                description = "Changement de langue instantané avec prise en charge complète du sens de lecture droite-à-gauche pour l'arabe."
            ),
            FeatureItem(
                title = "Modes Clair et Sombre adaptatifs",
                description = "Thèmes soignés respectant les préférences du système avec contraste accessible."
            ),
            FeatureItem(
                title = "Architecture fournisseur modulaire",
                description = "Conçu autour de l'interface VideoProvider avec Facebook en v1.0 et prêt pour l'intégration future de TikTok."
            )
        ),
        faqTitle = "Questions Fréquentes (FAQ)",
        faqSubtitle = "Informations claires sur les liens compatibles, les limites techniques et la confidentialité.",
        faqs = listOf(
            FaqItem(
                question = "Comment télécharger une vidéo Facebook prise en charge ?",
                answer = "Copiez le lien public de la vidéo, collez-le dans le champ ci-dessus, appuyez sur « Analyser la vidéo », sélectionnez la qualité souhaitée puis lancez le téléchargement MP4."
            ),
            FaqItem(
                question = "Quels types de liens Facebook sont acceptés ?",
                answer = "Les formats facebook.com/watch/?v=..., www.facebook.com/watch/?v=..., fb.watch/..., facebook.com/share/v/..., facebook.com/reel/... et les permaliens vidéo publics."
            ),
            FaqItem(
                question = "Pourquoi certaines vidéos ne peuvent-elles pas être récupérées ?",
                answer = "Les vidéos privées, réservées aux amis, soumises à une limite d'âge, protégées par DRM ou nécessitant une authentification Facebook ne sont pas accessibles. Nous ne contournons jamais ces protections."
            ),
            FaqItem(
                question = "Quelles options de qualité sont proposées ?",
                answer = "Les qualités dépendent des flux publiés par Facebook pour chaque vidéo (généralement MP4 HD 720p/1080p et SD 360p/480p)."
            ),
            FaqItem(
                question = "Le service conserve-t-il les liens soumis ou les vidéos ?",
                answer = "Aucune base de données externe ne conserve vos liens. Les fichiers temporaires partiels sont nettoyés automatiquement et le MP4 final est stocké sur votre appareil."
            ),
            FaqItem(
                question = "Comment respecter le droit d'auteur ?",
                answer = "Téléchargez uniquement les vidéos dont vous êtes l'auteur, pour lesquelles vous disposez d'une autorisation explicite ou diffusées sous licence libre."
            )
        ),
        securityArchitectureTitle = "Spécifications de Sécurité, Contrat API & Déploiement",
        securityArchitectureBullets = listOf(
            "Périmètre : FacebookVideoProvider actif en v1.0 ; TikTokVideoProviderStub enregistré dans ProviderRegistry.",
            "Défense SSRF : Rejet de localhost, RFC1918, 169.254.169.254, IP littérales et validation de chaque redirection HTTP 3xx.",
            "Intégrité média : Rejet des réponses text/html invalides, limite de 500 Mo, streaming par blocs de 32 Ko et empreinte SHA-256.",
            "Limitation de débit : Fenêtre glissante (8 requêtes / 60s) contre les abus automatisés.",
            "Fichiers temporaires : Écriture atomique via fichiers .part et nettoyage automatique après 30 minutes."
        ),
        privacyPolicyTitle = "Politique de Confidentialité",
        privacyPolicyBody = "VeloStream traite l'URL soumise uniquement pour extraire les métadonnées publiques et télécharger le flux MP4 choisi. Vos préférences de langue et de thème sont sauvegardées localement. Aucun cookie de compte ni donnée personnelle n'est collecté.",
        termsOfUseTitle = "Conditions d'Utilisation",
        termsOfUseBody = "L'utilisateur est seul responsable du respect des droits d'auteur et des conditions d'utilisation des contenus téléchargés. Toute tentative de contournement des mesures techniques de protection est interdite.",
        footerDisclaimer = "Mentions légales : VeloStream est un outil indépendant et n'est ni affilié, ni approuvé, ni sponsorisé par Meta Platforms, Inc. ou Facebook.",
        footerContactPlaceholder = "Contact & Sécurité : contact@velostream-example.invalid (Exemple)",
        closeButton = "Fermer"
    )
}
