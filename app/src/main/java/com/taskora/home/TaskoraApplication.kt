package com.taskora.home

import android.app.Application

/**
 * Application class. Kept intentionally minimal — no background work, services,
 * networking, or third-party initialization. All state lives in DataStore and is
 * accessed through the repository/ViewModel.
 */
class TaskoraApplication : Application()
