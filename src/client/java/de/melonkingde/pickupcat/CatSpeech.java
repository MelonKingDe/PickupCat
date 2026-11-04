package de.melonkingde.pickupcat;

import java.util.Random;

/** All the things the cat can say. {item} and {count} are replaced at runtime. */
final class CatSpeech {
	private CatSpeech() {}

	/** Escalates the longer you go without picking anything up. */
	static final String[] HUNGRY = {
			"im hungry :(",
			"my tummy is rumbling...",
			"hello?? food??",
			"i haven't eaten in ages...",
			"feed me something shiny",
			"im wasting away up here :(",
	};

	static final String[] EAT_SHORT = {
			"nom nom", "yum!", "mmm, tasty", "more please!", "*purrs*", "crunchy!", "delicious!", "*happy chewing*",
	};

	static final String[] EAT_ITEM = {
			"mmm, {item}!", "{item}? my favourite!", "ooh, {item}!", "{item} tastes like profit", "nom, {item}",
	};

	static final String[] EAT_BIG = {
			"that's a LOT of {item}!", "so much {item}... i might burst", "{count}x {item}?! feast!", "a whole pile of {item}!",
	};

	static final String[] VOMIT = {
			"blegh...", "why did you drop that?!", "*hairball noises*", "i didn't want it anyway...",
			"bleh. take it back", "that was MY snack!", "{item}?? too spicy",
	};

	static final String[] FAST = {
			"too fast!!", "slow down, i have a tiny mouth", "*frantic chewing*", "so many snacks!",
	};

	static final String[] GREET = {
			"meow! any snacks?", "hi again!", "ready to snack", "oh, a new place! snacks?",
	};

	static final String[] CHATTER = {
			"is it bazaar flip o'clock?",
			"don't forget your booster cookie",
			"have you collected your minions?",
			"psst... the drop rate feels good today",
			"i could go for some enchanted fish",
			"did you check the auction house?",
			"*stares at the void*",
			"purr...",
			"i bet that sword is overpriced",
			"my whiskers sense a rare drop",
			"is it a good day for a slayer?",
			"just five more minutes of farming...",
			"i'm a cat, not a sack",
			"do you ever wonder who named it the rift?",
	};

	static String pick(String[] lines, Random rng) {
		return lines[rng.nextInt(lines.length)];
	}
}
