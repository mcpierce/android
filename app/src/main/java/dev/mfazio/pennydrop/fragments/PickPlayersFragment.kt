package dev.mfazio.pennydrop.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import dev.mfazio.pennydrop.databinding.FragmentPickPlayersBinding
import dev.mfazio.pennydrop.viewmodels.GameViewModel
import dev.mfazio.pennydrop.viewmodels.PickPlayersViewModel

class PickPlayersFragment : Fragment() {
    private val pickPlayersViewModel by activityViewModels<PickPlayersViewModel>()
    private val gameViewModel by activityViewModels<GameViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentPickPlayersBinding.inflate(inflater, container, false).apply {
            this.vm = pickPlayersViewModel

            this.buttonPlayGame.setOnClickListener {
                gameViewModel.startGame(pickPlayersViewModel.players.value?.filter { newPlayer ->
                    newPlayer.isIncluded.get()
                }?.map { newPlayer ->
                    newPlayer.toPlayer()
                } ?: emptyList())
            }
        }
        return binding.root
    }
}