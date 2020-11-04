package ru.otus.otuskotlin.catalogue.backend.logics.categories

import ru.otus.otuskotlin.catalogue.backend.common.contexts.CategoryContext
import ru.otus.otuskotlin.catalogue.backend.common.contexts.ContextStatus
import ru.otus.otuskotlin.catalogue.backend.common.errors.GeneralError
import ru.otus.otuskotlin.catalogue.backend.common.models.categories.CategoryDeleteStubCases
import ru.otus.otuskotlin.catalogue.backend.common.models.categories.CategoryModel
import ru.otus.otuskotlin.catalogue.backend.common.models.items.NoteModel
import ru.otus.otuskotlin.catalogue.backend.common.repositories.ICategoryRepository
import ru.otus.otuskotlin.catalogue.backend.handlers.cor.corProc
import ru.otus.otuskotlin.catalogue.backend.logics.handlers.prepareResponse
import ru.otus.otuskotlin.catalogue.backend.logics.handlers.setRepoByWorkMode
import java.time.LocalDate

class CategoryDeleteChain(
    private val categoryRepoTest: ICategoryRepository,
    private val categoryRepoProd: ICategoryRepository
) {

    suspend fun exec(ctx: CategoryContext) = chain.exec(ctx.apply {
        categoryRepoTest = this@CategoryDeleteChain.categoryRepoTest
        categoryRepoProd = this@CategoryDeleteChain.categoryRepoProd
    })

    companion object{
        private val chain = corProc<CategoryContext>{
            // pipeline init
            exec { status = ContextStatus.RUNNING }

            // set repo in context
            processor {
                exec(setRepoByWorkMode())
            }

            // stub handling
            processor {
                isMatchable { stubCDeleteCase != CategoryDeleteStubCases.NONE }

                handler {
                    isMatchable { stubCDeleteCase == CategoryDeleteStubCases.SUCCESS }

                    exec {
                        responseCategory = CategoryModel(
                            id = "stub-delete-category",
                            label = "Notes",
                            type = "notes",
                            children = mutableSetOf(CategoryModel(id = requestCategoryId, label = "Subdir")),
                            items = mutableSetOf(
                                NoteModel(
                                    id = "12",
                                    header = "My note",
                                    description = "Some note",
                                    preview = "qwerty"
                                )
                            ),
                            creationDate = LocalDate.of(2010, 6, 13)
                        ).apply { children.removeIf { it.id == requestCategoryId } }
                        status = ContextStatus.FINISHING
                    }
                }
            }

            // job with db
            handler {
                isMatchable { status == ContextStatus.RUNNING }
                exec {
                    try {
                        responseCategory = categoryRepo.delete(requestCategoryId)
                    }
                    catch (e: Throwable){
                        status = ContextStatus.FAILING
                        errors.add(GeneralError(code = "category-in-repo-delete-error", e = e))
                    }
                }
            }

            // answer preparing
            exec(prepareResponse())
        }
    }
}