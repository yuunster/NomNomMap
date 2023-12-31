package com.bignerdranch.android.nomnommap

import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.core.view.doOnLayout
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.navArgs
import com.bignerdranch.android.nomnommap.databinding.FragmentMealDetailBinding
import kotlinx.coroutines.launch
import java.io.File
import java.util.Date

class MealDetailFragment : Fragment() {
    private var _binding: FragmentMealDetailBinding? = null
    private val binding
        get() = checkNotNull(_binding) {
            "Cannot access binding because it is null. Is the view visible?"
        }
    private val args: MealDetailFragmentArgs by navArgs()

    private val mealDetailViewModel: MealDetailViewModel by viewModels {
        MealDetailViewModelFactory(args.mealId)
    }

    private val takePhoto = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { didTakePhoto: Boolean ->
        if (didTakePhoto && photoName != null) {
            mealDetailViewModel.updateMeal { oldMeal ->
                oldMeal.copy(photoFileName = photoName)
            }
        }
    }

    private var photoName: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding =
            FragmentMealDetailBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun updateUi(meal: Meal) {
        binding.apply {
            if (editMealName.text.toString() != meal.title) {
                editMealName.setText(meal.title)
            }
            if (editCalories.text.toString() != meal.calories.toString()) {
                meal.calories.let { editCalories.setText(it) }
            }
            if (editProteins.text.toString() != meal.proteins.toString()) {
                meal.proteins.let { editProteins.setText(it) }
            }
            if (editCarbs.text.toString() != meal.carbs.toString()) {
                meal.carbs.let { editCarbs.setText(it) }
            }
            if (editFats.text.toString() != meal.fats.toString()) {
                meal.fats.let { editFats.setText(it) }
            }
            updatePhoto(meal.photoFileName)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.apply {
            clearButton.setOnClickListener {
                editMealName.setText("")
                editCalories.setText("")
                editProteins.setText("")
                editCarbs.setText("")
                editFats.setText("")
            }

            submitButton.setOnClickListener {
                mealDetailViewModel.updateMeal { oldMeal ->
                    oldMeal.copy(
                        title = editMealName.text.toString(),
                        calories = editCalories.text.toString(),
                        proteins = editProteins.text.toString(),
                        carbs = editCarbs.text.toString(),
                        fats = editFats.text.toString()
                    )
                }
                activity?.onBackPressed()
            }

            editMealName.doOnTextChanged { text, _, _, _ ->
                mealDetailViewModel.updateMeal { oldMeal ->
                    oldMeal.copy(title = text.toString())
                }
            }
            editCalories.doOnTextChanged { text, _, _, _ ->
                mealDetailViewModel.updateMeal { oldMeal ->
                    oldMeal.copy(calories = text.toString())
                }
            }
            editProteins.doOnTextChanged { text, _, _, _ ->
                mealDetailViewModel.updateMeal { oldMeal ->
                    oldMeal.copy(proteins = text.toString())
                }
            }
            editCarbs.doOnTextChanged { text, _, _, _ ->
                mealDetailViewModel.updateMeal { oldMeal ->
                    oldMeal.copy(carbs = text.toString())
                }
            }
            editFats.doOnTextChanged { text, _, _, _ ->
                mealDetailViewModel.updateMeal { oldMeal ->
                    oldMeal.copy(fats = text.toString())
                }
            }

            pictureButton.setOnClickListener {
                photoName = "IMG_${Date()}.JPG"
                val photoFile = File(requireContext().applicationContext.filesDir,
                    photoName)
                val photoUri = FileProvider.getUriForFile(
                    requireContext(),
                    "com.bignerdranch.android.nomnommap.fileprovider",
                    photoFile
                )
                takePhoto.launch(photoUri)
            }

            val captureImageIntent = takePhoto.contract.createIntent(
                requireContext(),
                Uri.EMPTY
            )
            pictureButton.isEnabled = canResolveIntent(captureImageIntent)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                mealDetailViewModel.meal.collect { meal ->
                    meal?.let { updateUi(it) }
                }
            }
        }
    }

    private fun canResolveIntent(intent: Intent): Boolean {
        val packageManager: PackageManager = requireActivity().packageManager
        val resolvedActivity: ResolveInfo? =
            packageManager.resolveActivity(
                intent,
                PackageManager.MATCH_DEFAULT_ONLY
            )
        return resolvedActivity != null
    }

    private fun updatePhoto(photoFileName: String?) {
        if (binding.mealPicture.tag != photoFileName) {
            val photoFile = photoFileName?.let {
                File(requireContext().applicationContext.filesDir, it)
            }
            if (photoFile?.exists() == true) {
                binding.mealPicture.doOnLayout { measuredView ->
                    val scaledBitmap = getScaledBitmap(
                        photoFile.path,
                        measuredView.width,
                        measuredView.height
                    )
                    binding.mealPicture.setImageBitmap(scaledBitmap)
                    binding.mealPicture.tag = photoFileName
                }
            } else {
                binding.mealPicture.tag = null
            }
        }
    }
}