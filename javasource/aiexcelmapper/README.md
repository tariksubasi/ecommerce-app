# AIExcelMapper

Tek bir Java action ile Excel Importer template'inin kolonlarini hedef entity'nin
attribute'larina esleyen modul. Yeni entity, yeni microflow, yeni jar gerekmez.

`JA_AIMapExcelTemplate` bastan sona her seyi yapar: kolonlari okur, Mx Model
Reflection'dan attribute listesini toplar, LLM'e sorar, gelen cevabi gercek
metamodele karsi dogrular, `ExcelImporter.Column` alanlarini yazar ve commit eder.
Geriye ne olup bittigini anlatan bir JSON rapor doner.

Hedef surum **Mendix 10.24**. Action, Studio Pro 10'un urettigi sekilde
`com.mendix.systemwideinterfaces.core.UserAction<String>` extend eder.
Harici jar yok — JSON okuyucu/yazici ve HTTP istemcisi dosyanin icinde.

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
                                      true, false, empty)   ->  $Report
   Show message $Report                      (istege bagli, debug icin)
   Refresh      $Template
   ```

   Loop, XPath, karar bloğu yok. Hepsi Java tarafinda.

6. **Buton** — Excel Importer'in Template detay sayfasina bir Call microflow
   butonu koy, caption `AI ile Eslestir`, microflow `ACT_AI_MapExcelTemplate`.

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
- Excel basliklari veri olarak muamele gorur: kontrol karakterleri temizlenir,
  200 karakterde kesilir ve sadece user mesajinda yer alir.
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
- **Ilk surumde yok**: Excel satir verisini LLM'e gonderme, reference mapping,
  enum value mapping, parse microflow secimi. Hicbiri bu surum icin gerekli degil.
