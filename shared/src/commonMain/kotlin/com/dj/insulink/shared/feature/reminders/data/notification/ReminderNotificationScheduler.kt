package com.dj.insulink.shared.feature.reminders.data.notification

// Faza 4 (Reminders notifikacije). Android-ov PRAVI Reminders ekran (app/feature/reminders) već
// ima potpuno ispravan, proveren notifikacioni lanac (ReminderScheduler/NotificationHelper/
// ReminderReceiver/BootReminderReceiver u :app, preko AlarmManager) - taj kod ostaje netaknut.
// :shared NE MOŽE zavisiti od :app (pogrešan smer zavisnosti), a dupliranje AlarmManager +
// BroadcastReceiver-a ovde bi zahtevalo nov unos u :shared-ov AndroidManifest (merge rizik, ne
// može se proveriti bez fizičkog Android uređaja trenutno povezanog) - za razliku od
// koristi/vrednosti (samo bi omogućilo da ISTI deljeni demo ekran na Android strani DODATNO
// zvoni, dok Android korisnik već ima pravu funkcionalnost preko svog pravog ekrana). Android
// actual je zato namerno no-op (isti princip kao Google Sign-In no-op na Android strani - vidi
// FirebaseAuthRepository.signInWithGoogle). iOS actual je stvarna implementacija preko
// UNUserNotificationCenter (sistemski framework, besplatan preko Kotlin/Native ObjC interop-a).
interface ReminderNotificationScheduler {
    suspend fun scheduleDaily(reminderId: Long, title: String, message: String, hour: Int, minute: Int)
    fun cancelReminder(reminderId: Long)
}
