package com.dj.insulink.shared.feature.auth.domain.model

// Tipizovana greška za shared Auth ekrane - nosi i sirov Firebase/Identity Toolkit error kod
// (npr. "EMAIL_EXISTS", "INVALID_LOGIN_CREDENTIALS", "WEAK_PASSWORD") kad je poznat, da bi UI
// mogao da prikaže smisleniju poruku nego generičko "greška".
class AuthException(
    message: String,
    val code: String? = null
) : Exception(message)
