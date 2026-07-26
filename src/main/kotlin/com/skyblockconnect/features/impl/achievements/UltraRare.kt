package com.skyblockconnect.features.impl.achievements

import com.skyblockconnect.utils.ChatUtils.removeFormatting

object UltraRare {

    private val mythological = listOf(
        "Daedalus Axe", "Crown of Greed", "Minos Relic", "Washed-up Souvenir",
        "Antique Remedies", "Dwarf Turtle Shelmet", "Griffin Feather", "Chimera",
    )

    private val slayer = listOf(

        "Scythe Blade", "Snake Rune", "Beheaded Horror", "Warden Heart", "Revenant Catalyst",

        "Digested Mosquito", "Fly Swatter", "Bite Rune", "Spider Catalyst",

        "Red Claw Egg", "Couture Rune", "Overflux Capacitor",

        "Judgement Core", "Etherwarp Merger", "Etherwarp Conduit", "Null Ovoid",
        "Sinful Dice", "Void Conqueror",

        "Wilson's Engineering Plans", "Subzero Inverter", "Archfiend Dice",
    )

    private val misc = listOf(
        "Necron's Handle", "Dark Claymore", "Giant's Sword", "Precursor Eye",
        "Golden Dragon", "Radioactive Vial", "Titanoboa Shed",
    )

    private val builtIn: List<Regex> =
        (mythological + slayer + misc).map { word(it) } +

            word("Dye")

    private fun word(term: String) = Regex("\\b${Regex.escape(term)}\\b", RegexOption.IGNORE_CASE)

    fun matches(text: String, extra: String = ""): Boolean {
        val clean = text.removeFormatting()
        if (builtIn.any { it.containsMatchIn(clean) }) return true

        return extra.split(',')
            .map(String::trim)
            .filter(String::isNotEmpty)
            .any { word(it).containsMatchIn(clean) }
    }
}
