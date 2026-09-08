export interface Flashcard {
  id: number;
  headword: string;
  reading: string;
  furigana: string; // e.g. 咲[さ]く
  meaning: string;
  partOfSpeech: string;
  exampleSentence: string;
  exampleReading: string;
  exampleMeaning: string;
  jlpt: string;
  dueIntervals: {
    again: string;
    hard: string;
    good: string;
    easy: string;
  };
}

export const SAMPLE_FLASHCARDS: Flashcard[] = [
  {
    id: 1,
    headword: "咲く",
    reading: "さく",
    furigana: "咲[さ]く",
    meaning: "to bloom, to blossom",
    partOfSpeech: "Godan verb (五段動詞)",
    exampleSentence: "春になると、桜の花が一斉に咲き乱れます。",
    exampleReading: "はるになると、さくらのはながいっせいにさきみだれます。",
    exampleMeaning: "When spring arrives, cherry blossoms bloom in all their glory.",
    jlpt: "N5",
    dueIntervals: { again: "< 1m", hard: "12m", good: "1d", easy: "4d" }
  },
  {
    id: 2,
    headword: "木漏れ日",
    reading: "こもれび",
    furigana: "木漏[こも]れ日[び]",
    meaning: "sunlight filtering through trees",
    partOfSpeech: "Noun (名詞)",
    exampleSentence: "静かな森の中を歩きながら、美しい木漏れ日を見上げた。",
    exampleReading: "しずかなもりのなかをあるきながら、うつくしいこもれびをみあげた。",
    exampleMeaning: "Walking through the quiet forest, I looked up at the sunlight filtering through trees.",
    jlpt: "N2",
    dueIntervals: { again: "< 1m", hard: "1d", good: "3d", easy: "7d" }
  },
  {
    id: 3,
    headword: "刹那",
    reading: "せつな",
    furigana: "刹那[せつな]",
    meaning: "moment, instant, fleeting split-second",
    partOfSpeech: "Noun / Adverb (名詞)",
    exampleSentence: "信号が変わったその刹那、彼は駆け出した。",
    exampleReading: "しんごうがかわったそのせつな、かれはかけだした。",
    exampleMeaning: "The very instant the traffic light changed, he sprinted forward.",
    jlpt: "N1",
    dueIntervals: { again: "< 1m", hard: "2d", good: "6d", easy: "14d" }
  },
  {
    id: 4,
    headword: "勉強",
    reading: "べんきょう",
    furigana: "勉[べん]強[きょう]",
    meaning: "study, diligence",
    partOfSpeech: "Suru verb / Noun (名詞・サ変)",
    exampleSentence: "毎朝、通勤電車の中で日本語を勉強しています。",
    exampleReading: "まいあさ、つうきんでんしゃのなかでにほんごをべんきょうしています。",
    exampleMeaning: "Every morning, I study Japanese on the commuter train.",
    jlpt: "N5",
    dueIntervals: { again: "< 1m", hard: "4d", good: "12d", easy: "28d" }
  },
  {
    id: 5,
    headword: "一期一会",
    reading: "いちごいちえ",
    furigana: "一期一会[いちごいちえ]",
    meaning: "once-in-a-lifetime encounter, treasure every meeting",
    partOfSpeech: "Yojijukugo (四字熟語)",
    exampleSentence: "旅先での出会いはまさに一期一会だと実感した。",
    exampleReading: "たびさきでのであいはまさにいちごいちえだとじっかんした。",
    exampleMeaning: "I realized that the people I met while traveling were truly once-in-a-lifetime encounters.",
    jlpt: "N1",
    dueIntervals: { again: "< 1m", hard: "5d", good: "18d", easy: "45d" }
  }
];
