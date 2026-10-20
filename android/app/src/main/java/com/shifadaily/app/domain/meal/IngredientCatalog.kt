package com.shifadaily.app.domain.meal

import com.shifadaily.app.domain.meal.Allergen.CELERY
import com.shifadaily.app.domain.meal.Allergen.EGGS
import com.shifadaily.app.domain.meal.Allergen.FISH
import com.shifadaily.app.domain.meal.Allergen.GLUTEN
import com.shifadaily.app.domain.meal.Allergen.MILK
import com.shifadaily.app.domain.meal.Allergen.SESAME
import com.shifadaily.app.domain.meal.Allergen.TREE_NUTS
import com.shifadaily.app.domain.meal.IngredientCategory.DAIRY
import com.shifadaily.app.domain.meal.IngredientCategory.DRINK
import com.shifadaily.app.domain.meal.IngredientCategory.FAT
import com.shifadaily.app.domain.meal.IngredientCategory.FRUIT
import com.shifadaily.app.domain.meal.IngredientCategory.HERB_SPICE
import com.shifadaily.app.domain.meal.IngredientCategory.LEGUME
import com.shifadaily.app.domain.meal.IngredientCategory.NUT
import com.shifadaily.app.domain.meal.IngredientCategory.PROTEIN
import com.shifadaily.app.domain.meal.IngredientCategory.STARCH
import com.shifadaily.app.domain.meal.IngredientCategory.SWEET
import com.shifadaily.app.domain.meal.IngredientCategory.VEGETABLE
import com.shifadaily.app.domain.meal.Tier.FREELY
import com.shifadaily.app.domain.meal.Tier.LIMITED
import com.shifadaily.app.domain.meal.Tier.NOT_LISTED
import com.shifadaily.app.domain.meal.Tier.OCCASIONAL

/**
 * Curated ingredient list. Tiers follow the user-supplied leaflet
 * "من أجل نظام غذائي متوازن" (Sothema). `pictured = true` means the item appears in the
 * leaflet's pictures; otherwise the tier is inferred from the leaflet text (e.g. "vegetables",
 * starch and legume equivalences, the note on red meat and egg yolk). See docs/FOOD-GUIDE-SOURCE.md.
 */
object IngredientCatalog {

    private fun i(
        key: String, en: String, ar: String, enDef: String, arDef: String,
        cat: IngredientCategory, tier: Tier, pictured: Boolean,
        allergens: Set<Allergen> = emptySet(), vegetarian: Boolean = true, staple: Boolean = false,
        syn: Array<String> = emptyArray(),
    ) = Ingredient(key, LText(en, ar), LText(enDef, arDef), cat, tier, pictured, allergens, vegetarian, syn.toList(), staple)

    val all: List<Ingredient> = listOf(
        // --- Vegetables: "as desired" (green panel) ---
        i("lettuce", "Lettuce", "خس", "lettuce", "الخس", VEGETABLE, FREELY, true, syn = arrayOf("laitue", "salade verte")),
        i("radish", "Radishes", "فجل", "radishes", "الفجل", VEGETABLE, FREELY, true, syn = arrayOf("radis", "radish")),
        i("cucumber", "Cucumber", "خيار", "cucumber", "الخيار", VEGETABLE, FREELY, true, syn = arrayOf("concombre", "cucumbers")),
        i("courgette", "Courgettes", "كوسة", "courgettes", "الكوسة", VEGETABLE, FREELY, true,
            syn = arrayOf("zucchini", "zucchinis", "courgette", "كوسا", "قرعة", "القرعة الخضراء")),
        i("bell_pepper", "Bell peppers", "فلفل حلو", "bell peppers", "الفلفل الحلو", VEGETABLE, FREELY, true,
            syn = arrayOf("pepper", "peppers", "poivron", "poivrons", "فلفل", "فلفلة")),
        i("onion", "Onions", "بصل", "onion", "البصل", VEGETABLE, FREELY, true, syn = arrayOf("onion", "oignon", "oignons", "بصلة")),
        i("tomato", "Tomatoes", "طماطم", "tomatoes", "الطماطم", VEGETABLE, FREELY, true,
            syn = arrayOf("tomato", "tomate", "tomates", "مطيشة", "بندورة")),
        i("lemon", "Lemons", "ليمون", "lemon juice", "عصير الليمون", VEGETABLE, FREELY, true, syn = arrayOf("lemon", "citron", "حامض")),
        i("cauliflower", "Cauliflower", "قرنبيط", "cauliflower", "القرنبيط", VEGETABLE, FREELY, true, syn = arrayOf("chou-fleur", "chou fleur", "شيفلور")),
        i("cabbage", "Cabbage", "ملفوف", "cabbage", "الملفوف", VEGETABLE, FREELY, true, syn = arrayOf("chou", "كرنب")),
        i("asparagus", "Asparagus", "هليون", "asparagus", "الهليون", VEGETABLE, FREELY, true, syn = arrayOf("asperges")),
        i("mushroom", "Mushrooms", "فطر", "mushrooms", "الفطر", VEGETABLE, FREELY, true, syn = arrayOf("mushroom", "champignons", "champignon", "فقاع")),
        i("brussels_sprouts", "Brussels sprouts", "كرنب بروكسل", "Brussels sprouts", "كرنب بروكسل", VEGETABLE, FREELY, true, syn = arrayOf("choux de bruxelles")),
        i("green_beans", "Green beans", "لوبيا خضراء", "green beans", "اللوبيا الخضراء", VEGETABLE, FREELY, true,
            syn = arrayOf("haricots verts", "لوبية خضراء", "فاصوليا خضراء")),
        i("eggplant", "Aubergines", "باذنجان", "aubergines", "الباذنجان", VEGETABLE, FREELY, true,
            syn = arrayOf("eggplant", "eggplants", "aubergine", "دنجال", "بادنجان")),
        i("artichoke", "Artichokes", "خرشوف", "artichokes", "الخرشوف", VEGETABLE, FREELY, true, syn = arrayOf("artichaut", "artichauts", "قوق")),
        i("pumpkin", "Pumpkin", "قرع", "pumpkin", "القرع", VEGETABLE, FREELY, true, syn = arrayOf("potiron", "citrouille", "squash", "يقطين", "قرع أحمر", "الكرعة")),
        // --- Vegetables not pictured: tier inferred from "vegetables" in the leaflet text ---
        i("spinach", "Spinach", "سبانخ", "spinach", "السبانخ", VEGETABLE, FREELY, false, syn = arrayOf("épinards", "epinards")),
        i("broccoli", "Broccoli", "بروكلي", "broccoli", "البروكلي", VEGETABLE, FREELY, false, syn = arrayOf("brocoli")),
        i("celery", "Celery", "كرفس", "celery", "الكرفس", VEGETABLE, FREELY, false, setOf(CELERY), syn = arrayOf("céleri", "celeri", "كرافس")),
        i("garlic", "Garlic", "ثوم", "garlic", "الثوم", VEGETABLE, FREELY, false, syn = arrayOf("ail", "توم")),
        i("turnip", "Turnips", "لفت", "turnip", "اللفت", VEGETABLE, FREELY, false, syn = arrayOf("navet", "navets", "turnip")),
        // --- Limited (yellow panel) ---
        i("carrot", "Carrots", "جزر", "carrots", "الجزر", VEGETABLE, LIMITED, true, syn = arrayOf("carrot", "carotte", "carottes", "خيزو")),
        i("peas", "Peas", "بازلاء (جلبانة)", "peas", "البازلاء", LEGUME, LIMITED, true, syn = arrayOf("petits pois", "جلبانة", "بازلاء")),
        i("chicken", "Chicken (skinless)", "دجاج (بدون جلد)", "chicken", "الدجاج", PROTEIN, LIMITED, true, vegetarian = false,
            syn = arrayOf("poulet", "دجاج", "djaj")),
        i("cheese", "Fresh cheese", "جبن طري", "cheese", "الجبن", DAIRY, LIMITED, true, setOf(MILK), syn = arrayOf("fromage", "جبن", "fromage frais")),
        i("milk", "Milk", "حليب", "milk", "الحليب", DAIRY, LIMITED, true, setOf(MILK), syn = arrayOf("lait", "حليب")),
        i("yogurt", "Plain yogurt (unsweetened)", "زبادي طبيعي (بدون سكر)", "plain yogurt", "الزبادي الطبيعي", DAIRY, LIMITED, true, setOf(MILK),
            syn = arrayOf("yaourt", "yoghurt", "yogurt", "رايب", "ياغورت", "زبادي")),
        i("pasta", "Whole-wheat pasta", "معكرونة كاملة", "pasta", "المعكرونة", STARCH, LIMITED, true, setOf(GLUTEN),
            syn = arrayOf("pâtes", "pates", "spaghetti", "معكرونة", "مقرونة")),
        i("olive_oil", "Olive oil", "زيت الزيتون", "olive oil", "زيت الزيتون", FAT, LIMITED, true, syn = arrayOf("huile d'olive", "huile", "oil", "زيت")),
        i("almonds", "Almonds", "لوز", "almonds", "اللوز", NUT, LIMITED, true, setOf(TREE_NUTS), syn = arrayOf("amandes", "almond")),
        i("walnuts", "Walnuts", "جوز", "walnuts", "الجوز", NUT, LIMITED, true, setOf(TREE_NUTS), syn = arrayOf("noix", "walnut", "كركاع")),
        i("apple", "Apples", "تفاح", "apple", "التفاح", FRUIT, LIMITED, true, syn = arrayOf("apple", "pomme", "pommes", "تفاحة")),
        i("orange", "Oranges", "برتقال", "orange", "البرتقال", FRUIT, LIMITED, true, syn = arrayOf("oranges", "ليمون حلو", "لتشين")),
        i("pear", "Pears", "إجاص", "pear", "الإجاص", FRUIT, LIMITED, true, syn = arrayOf("poire", "poires", "pear", "بوعويد")),
        i("kiwi", "Kiwis", "كيوي", "kiwi", "الكيوي", FRUIT, LIMITED, true, syn = arrayOf("kiwis")),
        i("strawberries", "Strawberries", "فراولة", "strawberries", "الفراولة", FRUIT, LIMITED, false, syn = arrayOf("fraises", "strawberry", "فريز")),
        i("banana", "Bananas", "موز", "banana", "الموز", FRUIT, LIMITED, true, syn = arrayOf("banane", "bananes", "banana")),
        i("melon", "Melon / watermelon", "بطيخ / دلاح", "melon", "البطيخ", FRUIT, LIMITED, true, syn = arrayOf("pastèque", "watermelon", "دلاح", "بطيخ")),
        i("peach", "Peaches", "خوخ", "peach", "الخوخ", FRUIT, LIMITED, true, syn = arrayOf("pêche", "peche", "peach")),
        // --- Limited: inferred from leaflet text (starch/legume equivalences, meat & egg-yolk note) ---
        i("bread", "Whole-wheat bread", "خبز كامل", "bread", "الخبز", STARCH, LIMITED, false, setOf(GLUTEN),
            syn = arrayOf("pain", "pain complet", "khobz", "خبز", "كسرة")),
        i("rice", "Rice", "أرز", "rice", "الأرز", STARCH, LIMITED, false, syn = arrayOf("riz", "روز")),
        i("potato", "Potatoes", "بطاطس", "potatoes", "البطاطس", STARCH, LIMITED, false,
            syn = arrayOf("potato", "pomme de terre", "pommes de terre", "بطاطا")),
        i("couscous", "Whole-grain couscous", "كسكس كامل", "couscous", "الكسكس", STARCH, LIMITED, false, setOf(GLUTEN), syn = arrayOf("semoule", "كسكسو", "سميدة")),
        i("oats", "Rolled oats", "شوفان", "oats", "الشوفان", STARCH, LIMITED, false, setOf(GLUTEN), syn = arrayOf("flocons d'avoine", "avoine", "oat")),
        i("lentils", "Lentils", "عدس", "lentils", "العدس", LEGUME, LIMITED, false, syn = arrayOf("lentilles", "lentil")),
        i("chickpeas", "Chickpeas", "حمص", "chickpeas", "الحمص", LEGUME, LIMITED, false,
            syn = arrayOf("chickpea", "pois chiches", "pois chiche", "حمص")),
        i("white_beans", "White beans", "فاصوليا بيضاء", "white beans", "الفاصوليا البيضاء", LEGUME, LIMITED, false,
            syn = arrayOf("haricots blancs", "لوبيا", "لوبية")),
        i("fava_beans", "Dried split fava beans", "فول مجفف مقشر", "fava beans", "الفول", LEGUME, LIMITED, false,
            syn = arrayOf("fèves", "feves", "fava", "broad beans", "فول")),
        i("eggs", "Eggs", "بيض", "eggs", "البيض", PROTEIN, LIMITED, false, setOf(EGGS), syn = arrayOf("egg", "oeufs", "œufs", "oeuf", "بيضة")),
        i("beef", "Lean beef", "لحم بقري قليل الدهن", "beef", "اللحم", PROTEIN, LIMITED, false, vegetarian = false,
            syn = arrayOf("boeuf", "viande", "meat", "لحم")),
        i("turkey", "Turkey", "ديك رومي", "turkey", "الديك الرومي", PROTEIN, LIMITED, false, vegetarian = false, syn = arrayOf("dinde", "بيبي")),
        i("butter", "Butter", "زبدة", "butter", "الزبدة", FAT, LIMITED, false, setOf(MILK), syn = arrayOf("beurre", "زبدة")),
        i("olives", "Olives", "زيتون", "olives", "الزيتون", FAT, LIMITED, false, syn = arrayOf("olive", "زيتون")),
        i("tahini", "Tahini", "طحينة", "tahini", "الطحينة", FAT, LIMITED, false, setOf(SESAME), syn = arrayOf("tahin", "crème de sésame")),
        // --- As desired: fish and unsweetened drinks (green panel) ---
        i("fish", "White fish fillets", "سمك أبيض", "fish", "السمك", PROTEIN, FREELY, true, setOf(FISH), vegetarian = false,
            syn = arrayOf("poisson", "سمك", "حوت", "merlan", "colin")),
        i("sardines", "Sardines", "سردين", "sardines", "السردين", PROTEIN, FREELY, false, setOf(FISH), vegetarian = false, syn = arrayOf("sardine", "سردينة")),
        i("water", "Water", "ماء", "water", "الماء", DRINK, FREELY, true, staple = true, syn = arrayOf("eau")),
        i("tea", "Tea (no sugar)", "شاي (بدون سكر)", "tea", "الشاي", DRINK, FREELY, true, syn = arrayOf("thé", "the", "أتاي", "شاي")),
        i("coffee", "Coffee (no sugar)", "قهوة (بدون سكر)", "coffee", "القهوة", DRINK, FREELY, true, syn = arrayOf("café", "cafe", "قهوة")),
        // --- Occasionally, small amounts (red panel) ---
        i("dates", "Dates", "تمر", "dates", "التمر", SWEET, OCCASIONAL, true, syn = arrayOf("dattes", "تمر")),
        i("sugar", "Sugar", "سكر", "sugar", "السكر", SWEET, OCCASIONAL, true, syn = arrayOf("sucre", "سكر")),
        i("honey", "Honey", "عسل", "honey", "العسل", SWEET, OCCASIONAL, true, syn = arrayOf("miel")),
        i("jam", "Jam", "مربى", "jam", "المربى", SWEET, OCCASIONAL, true, syn = arrayOf("confiture", "معجون")),
        i("sweets", "Sweets / candy", "حلويات / سكاكر", "sweets", "الحلويات", SWEET, OCCASIONAL, true, syn = arrayOf("bonbons", "candy", "حلوى")),
        i("pastries", "Cakes and pastries", "كعك ومعجنات حلوة", "pastries", "الحلويات المخبوزة", SWEET, OCCASIONAL, true, setOf(GLUTEN),
            syn = arrayOf("gâteau", "gateau", "cake", "pâtisserie", "حلوة")),
        i("ice_cream", "Ice cream", "مثلجات", "ice cream", "المثلجات", SWEET, OCCASIONAL, true, setOf(MILK), syn = arrayOf("glace", "آيس كريم", "لاكلاس")),
        i("chocolate", "Chocolate", "شوكولاتة", "chocolate", "الشوكولاتة", SWEET, OCCASIONAL, false, setOf(MILK), syn = arrayOf("chocolat", "شكلاط")),
        i("juice", "Fruit juice", "عصير الفواكه", "fruit juice", "عصير الفواكه", DRINK, OCCASIONAL, true, syn = arrayOf("jus", "عصير")),
        i("soda", "Soft drinks", "مشروبات غازية", "soft drinks", "المشروبات الغازية", DRINK, OCCASIONAL, true, syn = arrayOf("soda", "coca", "gazeuse", "مونادا")),
        // --- Not covered by the leaflet: herbs, spices, seasoning ---
        i("parsley", "Parsley", "بقدونس", "parsley", "البقدونس", HERB_SPICE, NOT_LISTED, false, syn = arrayOf("persil", "معدنوس")),
        i("coriander", "Fresh coriander", "كزبرة", "coriander", "الكزبرة", HERB_SPICE, NOT_LISTED, false, syn = arrayOf("coriandre", "cilantro", "قزبر", "قصبر")),
        i("mint", "Mint", "نعناع", "mint", "النعناع", HERB_SPICE, NOT_LISTED, false, syn = arrayOf("menthe", "نعناع")),
        i("cumin", "Cumin", "كمون", "cumin", "الكمون", HERB_SPICE, NOT_LISTED, false, staple = true, syn = arrayOf("cumin", "كامون")),
        i("paprika", "Paprika", "فلفل أحمر مطحون (تحميرة)", "paprika", "الفلفل الأحمر المطحون", HERB_SPICE, NOT_LISTED, false, staple = true,
            syn = arrayOf("paprika", "tahmira", "تحميرة")),
        i("turmeric", "Turmeric", "كركم", "turmeric", "الكركم", HERB_SPICE, NOT_LISTED, false, staple = true, syn = arrayOf("curcuma", "خرقوم")),
        i("ginger", "Ground ginger", "زنجبيل مطحون", "ginger", "الزنجبيل", HERB_SPICE, NOT_LISTED, false, staple = true, syn = arrayOf("gingembre", "سكين جبير")),
        i("black_pepper", "Black pepper", "فلفل أسود", "black pepper", "الفلفل الأسود", HERB_SPICE, NOT_LISTED, false, staple = true,
            syn = arrayOf("poivre", "poivre noir", "إبزار", "ابزار")),
        i("cinnamon", "Cinnamon", "قرفة", "cinnamon", "القرفة", HERB_SPICE, NOT_LISTED, false, staple = true, syn = arrayOf("cannelle", "قرفة")),
        i("salt", "Salt", "ملح", "salt", "الملح", HERB_SPICE, NOT_LISTED, false, staple = true, syn = arrayOf("sel")),
    )

    private val byKey = all.associateBy { it.key }

    operator fun get(key: String): Ingredient? = byKey[key]

    fun require(key: String): Ingredient = byKey[key] ?: error("Unknown ingredient: $key")

    fun byTier(tier: Tier): List<Ingredient> = all.filter { it.tier == tier }

    /** Default quantity per serving when the user adds an ingredient to a recipe. */
    fun defaultAddQty(ing: Ingredient): Pair<Double?, QtyUnit> = when (ing.category) {
        VEGETABLE -> 80.0 to QtyUnit.G
        FRUIT -> 1.0 to QtyUnit.PIECE
        PROTEIN -> if (ing.key == "eggs") 1.0 to QtyUnit.PIECE else 100.0 to QtyUnit.G
        LEGUME -> 60.0 to QtyUnit.G
        STARCH -> if (ing.key == "bread") 1.0 to QtyUnit.SLICE else 50.0 to QtyUnit.G
        DAIRY -> 60.0 to QtyUnit.G
        FAT -> 1.0 to QtyUnit.TSP
        NUT -> 10.0 to QtyUnit.G
        HERB_SPICE -> null to QtyUnit.TO_TASTE
        DRINK -> null to QtyUnit.TO_TASTE
        SWEET -> null to QtyUnit.TO_TASTE
    }
}
