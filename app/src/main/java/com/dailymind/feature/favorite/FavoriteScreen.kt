package com.dailymind.feature.favorite

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun FavoriteScreen(vm: FavoriteViewModel = hiltViewModel()) {
    val list by vm.favorites.collectAsStateWithLifecycle()
    LazyColumn { items(list) { Text(it.content) } }
}
