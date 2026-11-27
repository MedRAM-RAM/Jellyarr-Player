package com.jellyarr.player.data.stremio

import com.jellyarr.player.data.SettingsStore
import com.jellyarr.player.data.StoredAddon
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.json.JSONObject

class AddonRepository(private val settings: SettingsStore, scope: CoroutineScope) {

    val addons: StateFlow<List<Addon>> = settings.state
        .map { s -> s.addons.mapNotNull(::decode) }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    val enabled: List<Addon> get() = addons.value.filter { it.enabled }

    private fun decode(s: StoredAddon): Addon? = runCatching {
        Addon(s.transportUrl, AddonManifest.parse(JSONObject(s.manifestJson)), s.enabled)
    }.getOrNull()

    suspend fun install(url: String): Addon {
        val normalized = StremioClient.normalizeManifestUrl(url)
        val (raw, manifest) = StremioClient.fetchManifest(normalized)
        settings.update { st ->
            val existing = st.addons.indexOfFirst { it.transportUrl == normalized }
            val entry = StoredAddon(normalized, raw, true)
            val list = st.addons.toMutableList()
            if (existing >= 0) list[existing] = entry else list += entry
            st.copy(addons = list)
        }
        return Addon(normalized, manifest, true)
    }

    suspend fun refresh(addon: Addon) {
        val (raw, _) = StremioClient.fetchManifest(addon.transportUrl)
        settings.update { st ->
            st.copy(addons = st.addons.map { if (it.transportUrl == addon.transportUrl) it.copy(manifestJson = raw) else it })
        }
    }

    suspend fun remove(addon: Addon) = settings.update { st ->
        st.copy(addons = st.addons.filterNot { it.transportUrl == addon.transportUrl })
    }

    suspend fun setEnabled(addon: Addon, enabled: Boolean) = settings.update { st ->
        st.copy(addons = st.addons.map { if (it.transportUrl == addon.transportUrl) it.copy(enabled = enabled) else it })
    }

    suspend fun move(addon: Addon, delta: Int) = settings.update { st ->
        val list = st.addons.toMutableList()
        val i = list.indexOfFirst { it.transportUrl == addon.transportUrl }
        val j = (i + delta).coerceIn(0, list.lastIndex)
        if (i >= 0 && i != j) {
            val item = list.removeAt(i)
            list.add(j, item)
        }
        st.copy(addons = list)
    }

    fun forResource(resource: String, type: String, id: String): List<Addon> =
        enabled.filter { it.manifest.supports(resource, type, id) }
}
