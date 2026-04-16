package com.territorywars.presentation.navigation

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Login : Screen("login")
    data object Register : Screen("register")
    data object Map : Screen("map")
    data object Profile : Screen("profile")
    data object EditProfile : Screen("edit_profile")
    data object Leaderboard : Screen("leaderboard")
    data object Clan : Screen("clan")
    data object ClanCreate : Screen("clan_create")
    data object ClanDetail : Screen("clan_detail/{clanId}") {
        fun createRoute(clanId: String) = "clan_detail/$clanId"
    }
}
