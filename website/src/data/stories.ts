export interface StoryWord {
  word: string;
  furigana?: string;
  meaning: string;
  isVocab: boolean; // highlighted as card from Anki
}

export interface GradedStory {
  id: string;
  title: string;
  level: string;
  targetCount: number;
  tokens: StoryWord[];
  englishTranslation: string;
}

export const SAMPLE_STORIES: GradedStory[] = [
  {
    id: "spring-walk",
    title: "春の小さな出会い (A Small Spring Encounter)",
    level: "N4 ~ N3 Graded Passage",
    targetCount: 4,
    tokens: [
      { word: "暖かい", furigana: "あたたかい", meaning: "warm (weather)", isVocab: false },
      { word: "午後", furigana: "ごご", meaning: "afternoon", isVocab: false },
      { word: "、公園", furigana: "こうえん", meaning: "park", isVocab: false },
      { word: "の", meaning: "possessive particle", isVocab: false },
      { word: "小道", furigana: "こみち", meaning: "path, trail", isVocab: false },
      { word: "を", meaning: "object particle", isVocab: false },
      { word: "歩いていた", furigana: "あるいていた", meaning: "was walking", isVocab: false },
      { word: "。", meaning: "", isVocab: false },
      { word: "木漏れ日", furigana: "こもれび", meaning: "sunlight filtering through trees", isVocab: true },
      { word: "が", meaning: "subject particle", isVocab: false },
      { word: "地面", furigana: "じめん", meaning: "ground surface", isVocab: false },
      { word: "に", meaning: "direction particle", isVocab: false },
      { word: "優しく", furigana: "やさしく", meaning: "gently", isVocab: false },
      { word: "落ちて", furigana: "おちて", meaning: "falling", isVocab: false },
      { word: "いた", meaning: "was doing", isVocab: false },
      { word: "。", meaning: "", isVocab: false },
      { word: "ふと", meaning: "suddenly, casually", isVocab: false },
      { word: "見上げると", furigana: "みあげると", meaning: "looking up", isVocab: false },
      { word: "、桜の花", furigana: "さくらのはな", meaning: "cherry blossom flower", isVocab: false },
      { word: "が", meaning: "subject particle", isVocab: false },
      { word: "美しく", furigana: "うつくしく", meaning: "beautifully", isVocab: false },
      { word: "咲いて", furigana: "さいて", meaning: "blooming (verb: 咲く)", isVocab: true },
      { word: "いた", meaning: "was doing", isVocab: false },
      { word: "。", meaning: "", isVocab: false },
      { word: "風", furigana: "かぜ", meaning: "wind", isVocab: false },
      { word: "が", meaning: "subject particle", isVocab: false },
      { word: "吹いた", furigana: "ふいた", meaning: "blew", isVocab: false },
      { word: "その", meaning: "that", isVocab: false },
      { word: "刹那", furigana: "せつな", meaning: "fleeting instant, moment", isVocab: true },
      { word: "、花びら", furigana: "はなびら", meaning: "petals", isVocab: false },
      { word: "が", meaning: "subject particle", isVocab: false },
      { word: "舞い上がった", furigana: "まいあがった", meaning: "danced up in the air", isVocab: false },
      { word: "。", meaning: "", isVocab: false },
      { word: "これこそ", meaning: "truly this", isVocab: false },
      { word: "一期一会", furigana: "いちごいちえ", meaning: "once-in-a-lifetime treasure encounter", isVocab: true },
      { word: "の", meaning: "particle", isVocab: false },
      { word: "景色", furigana: "けしき", meaning: "scenery", isVocab: false },
      { word: "だと", meaning: "quote particle", isVocab: false },
      { word: "思った", furigana: "おもった", meaning: "thought", isVocab: false },
      { word: "。", meaning: "", isVocab: false }
    ],
    englishTranslation: "On a warm afternoon, I was walking down a path in the park. Sunlight filtering through the trees fell gently upon the ground. Casually looking up, cherry blossom flowers were blooming exquisitely. In the fleeting split-second that the wind blew, petals fluttered into the sky. I felt this scenery was truly a once-in-a-lifetime treasure encounter."
  }
];
