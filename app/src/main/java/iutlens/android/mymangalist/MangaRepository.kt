package iutlens.android.mymangalist

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import iutlens.android.mymangalist.model.Serie
import java.io.File

object MangaRepository {

    private const val LIBRARY_FILE = "manga.json"
    private const val PREFS_NAME   = "manga_prefs"
    private val gson = Gson()

    // ─── CATALOGUE (api_manga.json, res/raw, lecture seule) ───────────────────

    fun loadCatalogue(context: Context): List<Serie> {
        val input = context.resources.openRawResource(R.raw.api_manga)
        val json  = input.bufferedReader().use { it.readText() }
        val type  = object : TypeToken<List<Serie>>() {}.type
        return gson.fromJson(json, type)
    }

    // ─── BIBLIOTHÈQUE (manga.json, filesDir) ──────────────────────────────────

    fun loadLibrary(context: Context): MutableList<Serie> {
        val file = File(context.filesDir, LIBRARY_FILE)
        if (!file.exists()) return mutableListOf()
        val type = object : TypeToken<MutableList<Serie>>() {}.type
        return gson.fromJson(file.readText(), type)
    }

    fun saveLibrary(context: Context, list: List<Serie>) {
        File(context.filesDir, LIBRARY_FILE).writeText(gson.toJson(list))
    }

    fun addToLibrary(context: Context, serie: Serie) {
        val library = loadLibrary(context)
        if (library.none { it.id == serie.id }) {
            library.add(serie)
            saveLibrary(context, library)
        }
    }

    /**
     * Synchronise la bibliothèque avec le catalogue :
     * - Met à jour titre, coverUrl, status et la liste des volumes/chapitres
     *   pour chaque série déjà dans la bibliothèque (sans toucher aux états cochés).
     * - N'ajoute PAS automatiquement les nouvelles séries du catalogue
     *   (l'utilisateur les ajoute manuellement via AddMangaActivity).
     *
     * Appelé au chargement dans MainActivity et MangaVolumesActivity.
     */
    fun syncLibraryWithCatalogue(context: Context) {
        val catalogue = loadCatalogue(context)
        val library   = loadLibrary(context)
        if (library.isEmpty()) return

        var changed = false

        val updated = library.map { localSerie ->
            val catalogueSerie = catalogue.find { it.id == localSerie.id }

            if (catalogueSerie == null) {
                // Série supprimée du catalogue → on la garde telle quelle
                localSerie
            } else {
                val titleChanged   = localSerie.title    != catalogueSerie.title
                val coverChanged   = localSerie.coverUrl != catalogueSerie.coverUrl
                val statusChanged  = localSerie.status   != catalogueSerie.status
                val volumesChanged = localSerie.volumes.size != catalogueSerie.volumes.size ||
                        catalogueSerie.volumes.any { cv ->
                            localSerie.volumes.none { lv -> lv.number == cv.number } ||
                                    cv.chapters.size != localSerie.volumes.find { it.number == cv.number }?.chapters?.size
                        }

                if (titleChanged || coverChanged || statusChanged || volumesChanged) {
                    changed = true

                    // On repart des volumes du catalogue (données fraîches)
                    // et on recopie les états isOwned/isRead depuis la bibliothèque locale
                    val mergedVolumes = catalogueSerie.volumes.map { catVolume ->
                        val localVolume = localSerie.volumes.find { it.number == catVolume.number }

                        catVolume.isOwned = localVolume?.isOwned ?: false
                        catVolume.isRead  = localVolume?.isRead  ?: false

                        catVolume.chapters.forEach { catChapter ->
                            val localChapter = localVolume?.chapters?.find { it.number == catChapter.number }
                            catChapter.isRead = localChapter?.isRead ?: false
                        }

                        catVolume
                    }.toMutableList()

                    catalogueSerie.copy(volumes = mergedVolumes)
                } else {
                    localSerie
                }
            }
        }.toMutableList()

        if (changed) saveLibrary(context, updated)
    }

    // ─── ÉTATS OWNED / READ (SharedPreferences) ────────────────────────────────

    fun saveVolumeOwned(context: Context, serieId: Int, volumeNumber: Int, owned: Boolean) {
        prefs(context).edit()
            .putBoolean("serie_${serieId}_volume_${volumeNumber}_owned", owned)
            .apply()
    }

    fun saveVolumeRead(context: Context, serieId: Int, volumeNumber: Int, read: Boolean) {
        prefs(context).edit()
            .putBoolean("serie_${serieId}_volume_${volumeNumber}_read", read)
            .apply()
    }

    fun saveChapterRead(context: Context, serieId: Int, volumeNumber: Int, chapterNumber: Int, read: Boolean) {
        prefs(context).edit()
            .putBoolean("serie_${serieId}_volume_${volumeNumber}_chapter_${chapterNumber}_read", read)
            .apply()
    }

    fun getVolumeOwned(context: Context, serieId: Int, volumeNumber: Int, default: Boolean = false): Boolean =
        prefs(context).getBoolean("serie_${serieId}_volume_${volumeNumber}_owned", default)

    fun getVolumeRead(context: Context, serieId: Int, volumeNumber: Int, default: Boolean = false): Boolean =
        prefs(context).getBoolean("serie_${serieId}_volume_${volumeNumber}_read", default)

    fun getChapterRead(context: Context, serieId: Int, volumeNumber: Int, chapterNumber: Int, default: Boolean = false): Boolean =
        prefs(context).getBoolean("serie_${serieId}_volume_${volumeNumber}_chapter_${chapterNumber}_read", default)

    // ─── APPLICATION DES ÉTATS SUR UNE LISTE ──────────────────────────────────

    fun applyStates(context: Context, series: List<Serie>) {
        for (serie in series) {
            for (volume in serie.volumes) {
                volume.isOwned = getVolumeOwned(context, serie.id, volume.number, volume.isOwned)
                volume.isRead  = getVolumeRead(context, serie.id, volume.number, volume.isRead)

                for (chapter in volume.chapters) {
                    chapter.isRead = getChapterRead(
                        context, serie.id, volume.number, chapter.number, chapter.isRead
                    )
                }

                if (volume.chapters.isNotEmpty() && volume.chapters.all { it.isRead }) {
                    volume.isRead = true
                }
            }
        }
    }

    // ─── PRIVÉ ────────────────────────────────────────────────────────────────

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}