package eu.kanade.tachiyomi.ui.category

import androidx.compose.runtime.Immutable
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import dev.icerock.moko.resources.StringResource
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tachiyomi.domain.category.interactor.CreateCategoryWithName
import tachiyomi.domain.category.interactor.DeleteCategory
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.category.interactor.GetVisibleCategories
import tachiyomi.domain.category.interactor.HideCategory
import tachiyomi.domain.category.interactor.RenameCategory
import tachiyomi.domain.category.interactor.ReorderCategory
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.i18n.MR
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class AnimeCategoryScreenModel(
    private val getAllCategories: GetCategories = Injekt.get(),
    private val getVisibleCategories: GetVisibleCategories = Injekt.get(),
    private val createCategoryWithName: CreateCategoryWithName = Injekt.get(),
    private val hideCategory: HideCategory = Injekt.get(),
    private val deleteCategory: DeleteCategory = Injekt.get(),
    private val reorderCategory: ReorderCategory = Injekt.get(),
    private val renameCategory: RenameCategory = Injekt.get(),
    private val libraryPreferences: LibraryPreferences = Injekt.get(),
) : StateScreenModel<AnimeCategoryScreenState>(AnimeCategoryScreenState.Loading) {

    private val _events: Channel<AnimeCategoryEvent> = Channel()
    val events = _events.receiveAsFlow()

    init {
        screenModelScope.launch {
            val allCategories = if (libraryPreferences.hideHiddenCategoriesSettings().get()) {
                getVisibleCategories.subscribe()
            } else {
                getAllCategories.subscribe()
            }

            allCategories.collectLatest { categories ->
                mutableState.update {
                    AnimeCategoryScreenState.Success(
                        categories = categories
                            .filterNot(Category::isSystemCategory)
                            .toImmutableList(),
                    )
                }
            }
        }
    }

    fun createCategory(name: String) {
        screenModelScope.launch {
            when (createCategoryWithName.await(name)) {
                is CreateCategoryWithName.Result.InternalError -> _events.send(
                    AnimeCategoryEvent.InternalError,
                )

                else -> {}
            }
        }
    }

    fun hideCategory(category: Category) {
        screenModelScope.launch {
            when (hideCategory.await(category)) {
                is HideCategory.Result.InternalError -> _events.send(
                    AnimeCategoryEvent.InternalError,
                )
                else -> {}
            }
        }
    }

    fun deleteCategory(categoryId: Long) {
        screenModelScope.launch {
            when (deleteCategory.await(categoryId = categoryId)) {
                is DeleteCategory.Result.InternalError -> _events.send(
                    AnimeCategoryEvent.InternalError,
                )
                else -> {}
            }
        }
    }

    fun sortAlphabetically() {
        screenModelScope.launch {
            when (reorderCategory.sortAlphabetically()) {
                is ReorderCategory.Result.InternalError -> _events.send(AnimeCategoryEvent.InternalError)
                else -> {}
            }
        }
    }

    fun moveUp(category: Category) {
        screenModelScope.launch {
            when (reorderCategory.moveUp(category)) {
                is ReorderCategory.Result.InternalError -> _events.send(
                    AnimeCategoryEvent.InternalError,
                )
                else -> {}
            }
        }
    }

    fun moveDown(category: Category) {
        screenModelScope.launch {
            when (reorderCategory.moveDown(category)) {
                is ReorderCategory.Result.InternalError -> _events.send(
                    AnimeCategoryEvent.InternalError,
                )
                else -> {}
            }
        }
    }

    fun renameCategory(category: Category, name: String) {
        screenModelScope.launch {
            when (renameCategory.await(category, name)) {
                is RenameCategory.Result.InternalError -> _events.send(
                    AnimeCategoryEvent.InternalError,
                )
                else -> {}
            }
        }
    }

    fun showDialog(dialog: AnimeCategoryDialog) {
        mutableState.update {
            when (it) {
                AnimeCategoryScreenState.Loading -> it
                is AnimeCategoryScreenState.Success -> it.copy(dialog = dialog)
            }
        }
    }

    fun dismissDialog() {
        mutableState.update {
            when (it) {
                AnimeCategoryScreenState.Loading -> it
                is AnimeCategoryScreenState.Success -> it.copy(dialog = null)
            }
        }
    }
}

sealed interface AnimeCategoryDialog {
    data object Create : AnimeCategoryDialog
    data object SortAlphabetically : AnimeCategoryDialog
    data class Rename(val category: Category) : AnimeCategoryDialog
    data class Delete(val category: Category) : AnimeCategoryDialog
}

sealed interface AnimeCategoryEvent {
    sealed class LocalizedMessage(val stringRes: StringResource) : AnimeCategoryEvent
    data object InternalError : LocalizedMessage(MR.strings.internal_error)
}

sealed interface AnimeCategoryScreenState {

    @Immutable
    data object Loading : AnimeCategoryScreenState

    @Immutable
    data class Success(
        val categories: ImmutableList<Category>,
        val dialog: AnimeCategoryDialog? = null,
    ) : AnimeCategoryScreenState {

        val isEmpty: Boolean
            get() = categories.isEmpty()
    }
}
