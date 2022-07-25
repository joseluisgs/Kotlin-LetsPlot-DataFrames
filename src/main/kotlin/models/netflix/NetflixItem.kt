package models.netflix

import org.jetbrains.kotlinx.dataframe.annotations.DataSchema
import java.time.LocalDate


@DataSchema
data class NetflixItem(
    val show_id: String = "",
    val type: String = "",
    val title: String = "",
    val director: String? = null,
    val cast: String? = null,
    val country: String? = null,
    val date_added: LocalDate? = null,
    val release_year: Int = 0,
    val rating: String? = null,
    val duration: String = "",
    val listed_in: String = "",
    val description: String = "",
)