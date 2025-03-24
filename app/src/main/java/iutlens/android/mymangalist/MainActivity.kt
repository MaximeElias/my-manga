package iutlens.android.mymangalist

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.widget.Spinner
import android.widget.ArrayAdapter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import iutlens.android.mymangalist.adapter.MangaAdapter
import iutlens.android.mymangalist.database.MangaDatabaseHelper
import iutlens.android.mymangalist.database.MangaDatabaseHelper.Companion.COLUMN_TITLE
import iutlens.android.mymangalist.database.importMangasFromJSON
import iutlens.android.mymangalist.databinding.ActivityMainBinding
import iutlens.android.mymangalist.model.Manga
import coil.load

class MainActivity : AppCompatActivity() {

    private lateinit var mangaAdapter: MangaAdapter
    private var mangaList: MutableList<Manga> = mutableListOf()
    private lateinit var dbHelper: MangaDatabaseHelper
    private lateinit var addButton: Button

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val buttonNotes = findViewById<Button>(R.id.buttonNotes)
        val buttonAnime = findViewById<Button>(R.id.buttonAnimes)

        buttonNotes.setOnClickListener {
            val intent = Intent(this, NotesActivity::class.java)
            startActivity(intent)
        }
        buttonAnime.setOnClickListener {
            val intent = Intent(this, AnimeActivity::class.java)
            startActivity(intent)
        }

        val searchView = findViewById<SearchView>(R.id.searchView)

        val searchMagIcon = searchView.findViewById<ImageView>(androidx.appcompat.R.id.search_mag_icon)
        searchMagIcon.setColorFilter(ContextCompat.getColor(this, R.color.white), PorterDuff.Mode.SRC_IN)

        val searchText = searchView.findViewById<EditText>(androidx.appcompat.R.id.search_src_text)
        searchText.setTextColor(ContextCompat.getColor(this, R.color.white))
        searchText.setHintTextColor(ContextCompat.getColor(this, R.color.white))

        searchView.setOnClickListener {
            searchView.setIconified(false)
        }

        dbHelper = MangaDatabaseHelper(this)
        addButton = findViewById(R.id.buttonAddSeries)

        addButton.setOnClickListener {
            showAddSeriesDialog()
        }

        // Configuration de la barre d'état
        setupStatusBar()

        // Importer les mangas depuis JSON si nécessaire
        importMangasFromJSON(this)

        // Récupération des mangas depuis la base de données
        mangaList = getMangasFromDB().toMutableList()

        // Configuration du RecyclerView
        mangaAdapter = MangaAdapter(mangaList) { manga -> showEditDialog(manga) }
        binding.recyclerViewMangas.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewMangas.adapter = mangaAdapter

        // Récupérer les valeurs
        updateTotalUI()

        // Configuration du SearchView
        setupSearchView()
    }

    private fun addSeriesToDatabase(
        title: String,
        volumeIntegral: Int,
        volumeOwned: Int,
        volumesRead: Int,
        chapitre: Int,
        state: String,
        coverUrl: String
    ) {
        if (title.isEmpty()) {
            Toast.makeText(this, "Le titre est obligatoire", Toast.LENGTH_SHORT).show()
            return
        }

        // Ajouter la série à la base de données en incluant l'URL
        val result = dbHelper.addManga(title, volumeIntegral, volumeOwned, volumesRead, chapitre, state, coverUrl)

        // Vérifier si l'ajout a réussi
        if (result != -1L) {
            Toast.makeText(this, "Manga ajouté avec succès", Toast.LENGTH_SHORT).show()

            // Rafraîchir la liste après l'ajout
            mangaList = getMangasFromDB().toMutableList()
            mangaAdapter.updateList(mangaList)  // Notifier l'adaptateur pour l'actualiser
            updateTotalUI()  // Mettre à jour les totaux

            // Mettre à jour le fichier JSON après l'ajout
            dbHelper.updateJSONFile()

        } else {
            Toast.makeText(this, "Erreur lors de l'ajout du manga", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupStatusBar() {
        window.statusBarColor = ContextCompat.getColor(this, R.color.searchBackgroundDark)
        val window: Window = window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = ContextCompat.getColor(this, R.color.statusBarColorDark)
    }

    private fun setupSearchView() {
        val searchView = findViewById<SearchView>(R.id.searchView)

        // Modifier la couleur de l'icône de la loupe
        val searchMagIconId = searchView.context.resources.getIdentifier(
            "android:id/search_mag_icon", null, null
        )
        val searchMagIcon = searchView.findViewById<ImageView>(searchMagIconId)
        searchMagIcon?.setColorFilter(ContextCompat.getColor(this, R.color.white), android.graphics.PorterDuff.Mode.SRC_IN)

        // Modifier la couleur du texte dans la barre de recherche et du hint
        val searchTextId = searchView.context.resources.getIdentifier(
            "android:id/search_src_text", null, null
        )
        val searchEditText = searchView.findViewById<EditText>(searchTextId)
        searchEditText?.setTextColor(ContextCompat.getColor(this, R.color.white)) // Couleur du texte saisi
        searchEditText?.setHintTextColor(ContextCompat.getColor(this, R.color.white)) // Couleur du hint

        // Modifier la couleur du bouton de fermeture (croix)
        val searchCloseButtonId = searchView.context.resources.getIdentifier(
            "android:id/search_close_btn", null, null
        )
        val searchCloseButton = searchView.findViewById<ImageView>(searchCloseButtonId)
        searchCloseButton?.setColorFilter(ContextCompat.getColor(this, R.color.white))

        // Assurer que la loupe et le texte sont blancs pendant l'interaction
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                // Applique la couleur de la loupe
                searchMagIcon?.setColorFilter(ContextCompat.getColor(this@MainActivity, R.color.white), android.graphics.PorterDuff.Mode.SRC_IN)

                // Applique la couleur du texte et du hint
                searchEditText?.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.white))
                searchEditText?.setHintTextColor(ContextCompat.getColor(this@MainActivity, R.color.white))

                // Applique les filtres de recherche
                filterMangas(newText)
                return true
            }
        })

        // Réinitialiser la couleur après la perte du focus
        searchView.setOnQueryTextFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                searchMagIcon?.setColorFilter(ContextCompat.getColor(this@MainActivity, R.color.white), android.graphics.PorterDuff.Mode.SRC_IN)
                searchEditText?.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.white))
                searchEditText?.setHintTextColor(ContextCompat.getColor(this@MainActivity, R.color.white))
            }
        }
    }

    fun updateTotalUI() {
        // Mise à jour des compteurs
        val totalTomesPossedes = dbHelper.getTotalTomesPossedes()
        val totalTomesLus = dbHelper.getTotalTomesLus()
        val totalSeries = dbHelper.getTotalSeries()

        // Récupération des TextView une seule fois
        val tomesPossedesCounter = findViewById<TextView>(R.id.tomesPossedesCounter)
        val tomesLusCounter = findViewById<TextView>(R.id.tomesLusCounter)
        val seriesCounter = findViewById<TextView>(R.id.seriesCounter)

        // Mise à jour du texte des TextView
        tomesPossedesCounter.text = "Possédés: $totalTomesPossedes"
        tomesLusCounter.text = "Lus: $totalTomesLus"
        seriesCounter.text = "Séries: $totalSeries"
    }

    private fun getMangasFromDB(): List<Manga> {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM mangas ORDER BY $COLUMN_TITLE ASC", null)
        val mangas = mutableListOf<Manga>()

        while (cursor.moveToNext()) {
            val id = cursor.getInt(cursor.getColumnIndexOrThrow("id"))
            val title = cursor.getString(cursor.getColumnIndexOrThrow("title"))
            val volumeIntegral = cursor.getInt(cursor.getColumnIndexOrThrow("volumeIntegral"))
            val volumesOwned = cursor.getInt(cursor.getColumnIndexOrThrow("volumesOwned"))
            val volumesRead = cursor.getInt(cursor.getColumnIndexOrThrow("volumesRead"))
            val chapitre = cursor.getInt(cursor.getColumnIndexOrThrow("chapitre"))
            val state = cursor.getString(cursor.getColumnIndexOrThrow("state"))
            val coverUrl = cursor.getString(cursor.getColumnIndexOrThrow("coverUrl"))

            mangas.add(Manga(id, title, volumeIntegral, volumesOwned, volumesRead, chapitre, state, coverUrl))
        }
        cursor.close()
        db.close()

        return mangas
    }

    private fun filterMangas(query: String?) {
        val filteredList = if (query.isNullOrEmpty()) {
            mangaList  // La liste originale
        } else {
            mangaList.filter { it.title.contains(query, ignoreCase = true) }
        }

        mangaAdapter.updateList(filteredList)  // Met à jour l'adaptateur avec la nouvelle liste
    }

    private fun showAddSeriesDialog() {
        val dialog = Dialog(this, R.style.MyAlertDialogTheme)

        val dialogView = layoutInflater.inflate(R.layout.dialog_add_series, null)
        dialog.setContentView(dialogView)

        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val editTextTitle = dialogView.findViewById<EditText>(R.id.editTextTitle)
        val editTextVolumeIntegral = dialogView.findViewById<EditText>(R.id.editTextVolumeIntegral)
        val editTextVolumeOwned = dialogView.findViewById<EditText>(R.id.editTextVolumeOwned)
        val editTextVolumesRead = dialogView.findViewById<EditText>(R.id.editTextVolumesRead)
        val editTextChapitre = dialogView.findViewById<EditText>(R.id.editTextChapitre)
        val spinnerState = dialogView.findViewById<Spinner>(R.id.spinnerState)
        val buttonCancel = dialogView.findViewById<Button>(R.id.buttonCancel)
        val editTextCoverUrl = dialogView.findViewById<EditText>(R.id.editTextCoverUrl)
        val buttonAdd = dialogView.findViewById<Button>(R.id.buttonAdd)

        // Appliquer le même ArrayAdapter que dans showEditDialog
        val stateAdapter = ArrayAdapter.createFromResource(
            this,
            R.array.state_options,
            R.layout.item_spinner
        )
        stateAdapter.setDropDownViewResource(R.layout.item_spinner_dropdown)
        spinnerState.adapter = stateAdapter

        buttonAdd.setOnClickListener {
            val title = editTextTitle.text.toString().trim()
            val volumeIntegral = editTextVolumeIntegral.text.toString().toIntOrNull() ?: 0
            val volumeOwned = editTextVolumeOwned.text.toString().toIntOrNull() ?: 0
            val volumesRead = editTextVolumesRead.text.toString().toIntOrNull() ?: 0
            val chapitre = editTextChapitre.text.toString().toIntOrNull() ?: 0
            val state = spinnerState.selectedItem.toString()
            val coverUrl = editTextCoverUrl.text.toString().trim()

            addSeriesToDatabase(title, volumeIntegral, volumeOwned, volumesRead, chapitre, state, coverUrl) // Passer l'URL à la fonction addSeriesToDatabase
            dialog.dismiss()
        }

        buttonCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun showEditDialog(manga: Manga) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_manga, null)

        val editTextTitle = dialogView.findViewById<EditText>(R.id.editTextTitle)
        val editTextVolumeIntegral = dialogView.findViewById<EditText>(R.id.editTextVolumeIntegral)
        val editTextVolumeOwned = dialogView.findViewById<EditText>(R.id.editTextVolumeOwned)
        val editTextVolumesRead = dialogView.findViewById<EditText>(R.id.editTextVolumesRead)
        val editTextChapitre = dialogView.findViewById<EditText>(R.id.editTextChapitre)
        val editTextCoverUrl = dialogView.findViewById<EditText>(R.id.editTextCoverUrl)
        val spinnerState = dialogView.findViewById<Spinner>(R.id.spinnerState)
        val coverImageView = dialogView.findViewById<ImageView>(R.id.coverUrl)
        val buttonDelete = dialogView.findViewById<Button>(R.id.buttonDelete)

        // Affichage des infos existantes
        editTextTitle.setText(manga.title)
        editTextVolumeIntegral.setText(manga.volumeIntegral.toString())
        editTextVolumeOwned.setText(manga.volumesOwned.toString())
        editTextVolumesRead.setText(manga.volumesRead.toString())
        editTextChapitre.setText(manga.chapitre.toString())
        editTextCoverUrl.setText(manga.coverUrl)

        // Chargement de l'image avec Coil
        coverImageView.load(manga.coverUrl) {
            placeholder(R.drawable.ic_launcher_foreground)
            error(R.drawable.ic_launcher_foreground)
        }

        // Configuration du Spinner
        val stateAdapter = ArrayAdapter.createFromResource(
            this,
            R.array.state_options,
            R.layout.item_spinner
        )
        stateAdapter.setDropDownViewResource(R.layout.item_spinner_dropdown)
        spinnerState.adapter = stateAdapter
        spinnerState.setSelection(stateAdapter.getPosition(manga.state))

        // Création du Dialog
        val alertDialog = AlertDialog.Builder(this, R.style.MyAlertDialogTheme)
            .setView(dialogView)
            .setPositiveButton("ENREGISTRER") { _, _ ->
                val newTitle = editTextTitle.text.toString()
                val newVolumeIntegral = editTextVolumeIntegral.text.toString().toIntOrNull() ?: manga.volumeIntegral
                val newVolumeOwned = editTextVolumeOwned.text.toString().toIntOrNull() ?: manga.volumesOwned
                val newVolumesRead = editTextVolumesRead.text.toString().toIntOrNull() ?: manga.volumesRead
                val newChapitre = editTextChapitre.text.toString().toIntOrNull() ?: manga.chapitre
                val newState = spinnerState.selectedItem.toString()
                val newCoverUrl = editTextCoverUrl.text.toString().ifEmpty { manga.coverUrl }

                // Mise à jour en base de données
                dbHelper.updateManga(manga.id, newTitle, newVolumeIntegral, newVolumeOwned, newVolumesRead, newState, newChapitre, newCoverUrl)

                // Création d'une copie mise à jour du manga
                val updatedManga = manga.copy(
                    title = newTitle,
                    volumeIntegral = newVolumeIntegral,
                    volumesOwned = newVolumeOwned,
                    volumesRead = newVolumesRead,
                    chapitre = newChapitre,
                    state = newState,
                    coverUrl = newCoverUrl // Mise à jour de coverUrl malgré le `val`
                )

                // Mise à jour dans la liste
                val index = mangaList.indexOf(manga)
                if (index != -1) {
                    mangaList[index] = updatedManga
                }

                mangaAdapter.notifyItemChanged(index)
                updateTotalUI()

                // Mise à jour de l'image de couverture dans l'UI
                coverImageView.load(newCoverUrl) {
                    placeholder(R.drawable.ic_launcher_foreground)
                    error(R.drawable.ic_launcher_foreground)
                }
            }
            .setNegativeButton("ANNULER", null)
            .create()

        alertDialog.show()

        // Gérer la suppression
        buttonDelete.setOnClickListener {
            AlertDialog.Builder(this, R.style.MyAlertDialogTheme)
                .setTitle("Supprimer la série")
                .setMessage("Voulez-vous vraiment supprimer ${manga.title} ?")
                .setPositiveButton("OUI") { _, _ ->
                    dbHelper.deleteManga(manga.id)
                    mangaList.remove(manga)
                    mangaAdapter.notifyDataSetChanged()
                    updateTotalUI()
                    alertDialog.dismiss()
                }
                .setNegativeButton("NON", null)
                .show()
        }
    }
}
