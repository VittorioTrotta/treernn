package com.example.expirationtracker

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.expirationtracker.databinding.ActivityMainBinding
import com.example.expirationtracker.db.ExpirationDatabase
import com.example.expirationtracker.model.Product
import com.example.expirationtracker.repository.ProductRepository
import com.example.expirationtracker.viewmodel.ProductViewModel
import com.example.expirationtracker.viewmodel.ProductViewModelFactory
import com.example.expirationtracker.viewmodel.UiState
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: ProductViewModel
    private lateinit var adapter: ProductAdapter
    private var speechRecognizer: SpeechRecognizer? = null

    private val requestMicPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startListening() else showSnackbar(getString(R.string.permission_denied))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViewModel()
        setupRecyclerView()
        setupFab()
        observeState()
    }

    private fun setupViewModel() {
        val dao = ExpirationDatabase.getInstance(this).productDao()
        val repository = ProductRepository(dao)
        viewModel = ViewModelProvider(this, ProductViewModelFactory(repository))[ProductViewModel::class.java]
    }

    private fun setupRecyclerView() {
        adapter = ProductAdapter { product -> viewModel.deleteProduct(product) }
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        // Swipe-to-delete
        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder) = false
            override fun onSwiped(vh: RecyclerView.ViewHolder, dir: Int) {
                val product = adapter.currentList[vh.adapterPosition]
                viewModel.deleteProduct(product)
                showSnackbar("\"${product.name}\" removido")
            }
        }).attachToRecyclerView(binding.recyclerView)

        lifecycleScope.launch {
            viewModel.products.collectLatest { products ->
                adapter.submitList(products)
                binding.emptyText.visibility = if (products.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    private fun setupFab() {
        binding.fabMic.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED
            ) startListening()
            else requestMicPermission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun observeState() {
        lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                when (state) {
                    is UiState.Idle -> {
                        binding.fabMic.setImageResource(android.R.drawable.ic_btn_speak_now)
                        binding.statusText.text = getString(R.string.hint_tap_mic)
                    }
                    is UiState.Listening -> {
                        binding.fabMic.setImageResource(android.R.drawable.presence_audio_online)
                        binding.statusText.text = getString(R.string.listening)
                    }
                    is UiState.Success -> {
                        binding.statusText.text = state.message
                        showSnackbar(state.message)
                        viewModel.resetState()
                    }
                    is UiState.Error -> {
                        binding.statusText.text = state.message
                        showSnackbar(state.message)
                        viewModel.resetState()
                    }
                }
            }
        }
    }

    private fun startListening() {
        speechRecognizer?.destroy()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { viewModel.setListening() }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {
                viewModel.processVoiceCommand("")
            }
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val transcript = matches?.firstOrNull() ?: ""
                binding.statusText.text = "\"$transcript\""
                viewModel.processVoiceCommand(transcript)
            }
            override fun onPartialResults(partial: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR")
            putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.speech_prompt))
        }
        speechRecognizer?.startListening(intent)
    }

    private fun showSnackbar(message: String) =
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer?.destroy()
    }
}

// ── RecyclerView adapter ──────────────────────────────────────────────────────

class ProductAdapter(
    private val onDelete: (Product) -> Unit
) : ListAdapter<Product, ProductAdapter.ViewHolder>(DIFF) {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.textName)
        val date: TextView = view.findViewById(R.id.textDate)
        val status: TextView = view.findViewById(R.id.textStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_product, parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val product = getItem(position)
        holder.name.text = product.name.replaceFirstChar { it.uppercaseChar() }
        holder.date.text = product.formattedExpirationDate()
        val ctx = holder.itemView.context
        when {
            product.isExpired() -> {
                holder.status.text = ctx.getString(R.string.status_expired)
                holder.status.setTextColor(ctx.getColor(android.R.color.holo_red_dark))
            }
            product.isExpiringSoon() -> {
                holder.status.text = ctx.getString(R.string.status_expiring_soon, product.daysUntilExpiration())
                holder.status.setTextColor(ctx.getColor(android.R.color.holo_orange_dark))
            }
            else -> {
                holder.status.text = ctx.getString(R.string.status_ok, product.daysUntilExpiration())
                holder.status.setTextColor(ctx.getColor(android.R.color.holo_green_dark))
            }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Product>() {
            override fun areItemsTheSame(a: Product, b: Product) = a.id == b.id
            override fun areContentsTheSame(a: Product, b: Product) = a == b
        }
    }
}
