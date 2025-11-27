import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinFeature.UseJavaDurationConversion
import com.fasterxml.jackson.module.kotlin.registerKotlinModule

internal val objectMapper = ObjectMapper()
    .registerKotlinModule { enable(UseJavaDurationConversion) }
    .registerModule(JavaTimeModule())