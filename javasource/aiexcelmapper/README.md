# AIExcelMapper

Tek bir Java action ile Excel Importer template'inin kolonlarini hedef entity'nin
attribute'larina esleyen modul. Yeni entity, yeni microflow, yeni jar gerekmez.

`JA_AIMapExcelTemplate` bastan sona her seyi yapar: kolonlari okur, Mx Model
Reflection'dan attribute listesini toplar, LLM'e sorar, gelen cevabi gercek
metamodele karsi dogrular, `ExcelImporter.Column` alanlarini yazar ve commit eder.
Geriye ne olup bittigini anlatan bir JSON rapor doner.

Hedef surum **Mendix 10.24**. Action, Studio Pro 10'un urettigi sekilde
`com.mendix.systemwideinterfaces.core.UserAction<String>` extend eder.
JSON okuyucu/yazici ve HTTP istemcisi dosyanin icinde; tek harici bagimlilik
sample deger okurken kullanilan Apache POI, o da zaten Excel Importer ile
`userlib`'de geliyor (`poi-4.1.2`, `poi-ooxml-4.1.2`). POI bulunamazsa action
patlamaz, sadece header ile devam eder.

## Studio Pro kurulumu

1. **Modul olustur** — App Explorer'da sag tik > Add module > adi tam olarak
   `AIExcelMapper` olsun (Java package adi `aiexcelmapper` buradan tureniyor).
2. **Java action ekle** — modulun icine sag tik > Add > Java action, adi
   `JA_AIMapExcelTemplate`. Studio Pro dosyayi uretecek; repodaki dosya zaten
   ayni yerde oldugu icin uzerine yazmaz, mevcut kodu okur.
3. **Parametreleri gir** (sira onemli, generate edilen constructor bu sirayi bekler):

   | # | Ad | Tip | Zorunlu |
   |---|---|---|---|
   | 1 | `TemplateObject` | Object — `ExcelImporter.Template` | evet |
   | 2 | `Endpoint` | String | evet |
   | 3 | `ApiKey` | String | hayir |
   | 4 | `ModelName` | String | evet |
   | 5 | `MinConfidence` | Decimal | hayir (bos → 0.80) |
   | 6 | `DefaultDateFormat` | String | hayir |
   | 7 | `OverwriteExisting` | Boolean | hayir (bos → true) |
   | 8 | `DryRun` | Boolean | hayir (bos → false) |
   | 9 | `TimeoutSeconds` | Integer/Long | hayir (bos → 120) |
   | 10 | `SampleRowCount` | Integer/Long | hayir (bos → 5, `0` → sadece header) |

   Return type: **String**.

4. **Constant'lar** (modul icinde, Endpoint/ApiKey/Model icin):

   | Constant | Ornek deger |
   |---|---|
   | `LLM_Endpoint` | `https://askjhdaksda.qwe.com.tr/api/chat/completions` |
   | `LLM_ApiKey` | `sk-...` |
   | `LLM_Model` | `qwen35-122b-a10b-fp8` |

   `LLM_ApiKey`'i production'da constant yerine environment'tan gelen bir
   custom setting olarak tutmak daha dogru.

5. **Cagiran microflow** — `ACT_AI_MapExcelTemplate`, tek parametre
   `$Template : ExcelImporter.Template`. Icinde sadece:

   ```
   Java action  JA_AIMapExcelTemplate($Template, @LLM_Endpoint, @LLM_ApiKey,
                                      @LLM_Model, empty, 'dd.MM.yyyy',
                                      true, false, empty, 5)   ->  $Report
   Show message $Report                      (istege bagli, debug icin)
   Refresh      $Template
   ```

   Loop, XPath, karar bloğu yok. Hepsi Java tarafinda.

6. **Buton** — Excel Importer'in Template detay sayfasina bir Call microflow
   butonu koy, caption `AI ile Eslestir`, microflow `ACT_AI_MapExcelTemplate`.

## Sample deger okuma

`SampleRowCount > 0` ise action, template'e bagli sample Excel dosyasini
(`TemplateDocument_Template`) POI ile acar ve `SheetIndex` / `FirstDataRowNumber`
ayarlarina gore ilk N data satirini okur. Hucreler Excel'de **gorundugu gibi**
alinir — tarih kolonu `15.03.2019` olarak gider, basinda sifir olan telefon
`0532 111 22 33` olarak kalir. Formul hucrelerinde cached sonuc kullanilir
(formul evaluate edilmez, POI'nin desteklemedigi fonksiyonda patlamasin diye).

LLM'e giden payload boyle olur:

```json
{"entity":"HR.Personel",
 "excelColumns":[
   {"columnNumber":0,"header":"Sicil Numarası",
    "sampleValues":["10023","10024","10025"],"samplesAllDistinct":true},
   {"columnNumber":2,"header":"İşe Giriş Tarihi",
    "sampleValues":["15.03.2019","01.07.2020"],"samplesAllDistinct":true}],
 "allowedAttributes":[{"name":"SicilNo","type":"String","keyable":true}]}
```

`samplesAllDistinct` key secimi icin en degerli sinyal: bir kolonun ornek
degerlerinin hepsi farkliysa identifier adayidir, tekrar ediyorsa degildir.

**Onemli — KVKK:** bu mod gercek hucre degerlerini LLM gateway'ine gonderir.
Personel entity'sinde bu gercek isim, telefon ve sicil demektir. Gateway'in
sirket ici oldugundan ve istekleri loglamadigindan emin ol. Istemiyorsan
`SampleRowCount = 0` ver, sistem sadece header ile calisir.

Sampling bir optimizasyon olarak ele alinir: dosya yoksa, bos ise, sheet
numarasi tutmuyorsa, 25MB'tan buyukse veya POI classpath'te degilse action
**patlamaz** — uyari yazar ve header-only devam eder.

## Karakter kodlamasi

Java dosyasi **tamamen ASCII**. Turkce harf iceren tek yer harf katlama tablosuydu,
o da artik kod noktasiyla yaziliyor (`case 0x0130:` gibi). Sebebi: Turkce Windows'ta
javac varsayilan olarak windows-1254 kullanir, Mendix dosyayi UTF-8 yazar, ve
dosyada Turkce karakter varsa "unmappable character" hatasi alirsin. Dosya
`-encoding US-ASCII` ve `-encoding windows-1254` ile ayri ayri derlenerek dogrulandi.

Kod yazarken dikkat: Java'da `\u` **yorum satirinda bile** lexer tarafindan unicode
escape olarak islenir. Yoruma `\uXXXX` yazarsan "illegal unicode escape" alirsin.
Cift ters bolu (`\\u`) guvenli.

Calisma aninda da iki koruma var:

- **Giden istek**: JSON yazici ASCII disindaki her karakteri `\uXXXX` olarak
  kaciriyor, yani request body JVM'den saf ASCII olarak cikiyor. Aradaki hicbir
  gateway, proxy veya log Turkce karakteri bozamiyor. Model zarfi acinca escape
  cozuluyor ve basligi dogru goruyor.
- **Gelen cevap**: `Content-Type` basligindaki charset dikkate aliniyor.
  Gateway windows-1254 veya ISO-8859-9 donerse UTF-8 varsayip bozmuyor.

## Studio Pro proxy alani

Entity tipli bir Object parametresi icin Studio Pro iki alan uretir:

```java
private IMendixObject __TemplateObject;                  // ham obje
private excelimporter.proxies.Template TemplateObject;   // proxy
```

Kullanici kodu bilerek **`__TemplateObject`** kullaniyor. Aksi halde Studio Pro
dosyayi yeniden urettiginde `TemplateObject` proxy tipine donusur ve
"incompatible types" derleme hatasi alirsin. Bu haliyle dosya hem oldugu gibi,
hem de Studio Pro proxy alanini ekledikten sonra derleniyor.

## Surum bagimsizligi

Excel Importer ve Mx Model Reflection major surumler arasinda member isimlerini
degistiriyor. Bu yuzden action hicbir ismi sabit tutmuyor: calisma aninda
metamodelden **ne isim tasidigina degil, neyi gosterdigine** bakarak cozuyor.

Ornegin hedef entity baglantisi once `Template_MxObjectType` olarak deneniyor;
yoksa `Core.getMetaAssociations()` icinden `Template` ile `MxObjectType`
arasindaki association bulunuyor. Association MxObjectType tarafindan
sahipleniliyorsa (yani Template'te member yoksa) XPath ile sorgulaniyor.
Reference mapping'e ait association'lar (`*_Reference`) bu aramada eleniyor,
boylece yanlislikla reference tarafina yazilmiyor.

Ayni sey Column, MxObjectMember ve MxObjectType member'lari icin de gecerli;
her biri icin birkac aday isim ve buyuk-kucuk harf toleransi var.

Rapor, o calismada neye baglandigini `resolvedSchema` altinda yaziyor:

```json
"resolvedSchema":{"templateObjectType":"Template_MxObjectType",
                  "columnMember":"Column_MxObjectMember",
                  "mappingType":"MappingType", ...}
```

Bir member gercekten cozulemezse hata, o entity'nin **sahip oldugu tum
member'lari listeliyor** — yani surum farki tek ekran goruntusunden teshis
edilebiliyor:

```
Could not find the mapping type (tried [MappingType, Type]) on
ExcelImporter.Column. ... Members present: [AttributeTypeEnum, ColNumber,
Column_MxObjectMember, ..., IsKey, IsReferenceKey, Text]
```

## Onkosullar

- Hedef entity'nin modulu **Mx Model Reflection** ekraninda synchronize edilmis
  olmali. Bu action attribute listesini oradan okur.
- Template'te **Mendix Object** secili olmali (`Template_MxObjectType`).
- Template'in kolonlari Excel Importer'in kendi "New template by Excel file"
  akisiyla olusturulmus olmali. Bu action kolon yaratmaz, sadece esler.

## Endpoint bicimi

`Endpoint` OpenAI uyumlu bir chat/completions adresi. Tam URL verilebilir,
taban URL de verilebilir — her ikisi de ayni yere cozulur:

```
https://host/api/chat/completions   ->  aynen kullanilir
https://host/v1                     ->  https://host/v1/chat/completions
https://host/api/                   ->  https://host/api/chat/completions
```

Gonderilen istek, ekrandaki curl ile birebir ayni sekilde:

```
POST <endpoint>
Content-Type: application/json
Authorization: Bearer <ApiKey>

{"model":"...","messages":[{"role":"system",...},{"role":"user",...}],
 "max_tokens":...,"stream":false,"temperature":0,
 "response_format":{"type":"json_object"}}
```

`temperature` veya `response_format`'i kabul etmeyen bir gateway HTTP 400
dondurursa, action o alani dusurup istegi tekrarlar — elle mudahale gerekmez.

## Hangi alanlar yaziliyor

Sadece duz attribute mapping'in okudugu alanlar:

| Alan | Deger |
|---|---|
| `MappingType` | `Attribute` |
| `DataSource` | `CellValue` |
| `Column_MxObjectType` | template'in hedef entity'si |
| `Column_MxObjectMember` | secilen attribute |
| `IsKey` | `Yes` / `No` |
| `IsReferenceKey` | `YesOnlyMainObject` / `NoKey` |
| `AttributeTypeEnum` | attribute'un tipi (DateTime mask'i icin sart) |
| `InputMask` | sadece DateTime attribute'ta ve alan bossa |
| `Status`, `Details` | modul surumunde varsa, bilgi amacli |

Gercek reference alanlarina **hic dokunulmuyor**: `Column_MxObjectReference`,
`Column_MxObjectMember_Reference`, `Column_MxObjectType_Reference`,
`ReferenceHandling`. `MappingType = Reference` olan bir kolon zaten tamamen
atlaniyor.

## Guvenlik kontrolleri

LLM'in soyledigi hicbir sey dogrudan yazilmiyor:

- Attribute adi once birebir, sonra `Entity.Attr` on ekini atarak, en son
  Turkce/buyuk-kucuk harf farklarini tolere eden bir normalize ile aranir —
  ve ancak tek bir eslesme varsa kabul edilir. Uydurulan ad reddedilir.
- Attribute gercekten yazilabilir mi diye Mendix metamodeline bakilir.
  AutoNumber, Binary ve calculated attribute'lar hedef olamaz.
- Key olarak isaretlenen alan Boolean/HashString/Binary ise key bayragi
  dusurulur, kolon normal olarak eslenir.
- Iki kolon ayni attribute'a giderse yuksek confidence kazanir, digeri
  `SKIPPED_DUPLICATE` olarak raporlanir.
- `MinConfidence` altindaki oneriler atlanir.
- Excel basliklari ve hucre degerleri veri olarak muamele gorur: kontrol
  karakterleri temizlenir, baslik 200 / hucre 80 karakterde kesilir ve sadece
  user mesajinda yer alir. System prompt'ta ikisinin de talimat olmadigi yazar.
- Hicbir batch'te commit yok; tum LLM cagrilari bittikten sonra tek seferde
  `Core.commit` cagrilir. Ortada kalan yarim yazma olmaz.

## Donen rapor

```json
{
  "ok": true,
  "entity": "HR.Personel",
  "mapped": 5,
  "unmapped": 4,
  "keys": ["SicilNo"],
  "llmCalls": 1,
  "durationMs": 104,
  "warnings": [],
  "results": [
    {"columnNumber":0,"header":"Sicil Numarası","status":"MAPPED",
     "attribute":"SicilNo","isKey":true,"confidence":"0.99"},
    {"columnNumber":6,"header":"Açıklama","status":"SKIPPED_UNKNOWN_ATTRIBUTE",
     "attribute":null,"isKey":false,"confidence":"0.91",
     "reason":"'Aciklama' is not an attribute of this entity"}
  ]
}
```

`status` degerleri: `MAPPED`, `SKIPPED_NO_SUGGESTION`, `SKIPPED_LOW_CONFIDENCE`,
`SKIPPED_UNKNOWN_ATTRIBUTE`, `SKIPPED_NOT_WRITABLE`, `SKIPPED_DUPLICATE`,
`SKIPPED_REFERENCE_MAPPING`, `SKIPPED_EXISTING`, `SKIPPED_EMPTY_HEADER`,
`SKIPPED_DUPLICATE_COLUMN`, `SKIPPED_INVALID`.

## Isletme notlari

- **Genis sayfalar**: 40 kolondan fazlasi otomatik olarak birden fazla LLM
  cagrisina bolunur. `Cfg.BATCH_SIZE` ile degistirilebilir.
- **Gecici hatalar**: 429 ve 5xx icin `Retry-After` dikkate alinarak jitter'li
  exponential backoff ile 4 denemeye kadar tekrar edilir.
- **Self-signed sertifika**: TLS hatasi retry edilmez; sertifikayi Mendix
  truststore'una eklemeni soyleyen net bir hata doner. Sertifika dogrulamasi
  hicbir kosulda kapatilmaz.
- **Once dene**: `DryRun = true` ile calistir, raporu oku, sonra gercek calistir.
- **Elle yapilan islerin korunmasi**: `OverwriteExisting = false` dersen zaten
  attribute atanmis kolonlara dokunulmaz.
- **Bu surumde yok**: reference mapping, enum value mapping, parse microflow
  secimi. Hicbiri bu surum icin gerekli degil.
