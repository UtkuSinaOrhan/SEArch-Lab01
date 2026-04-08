# LAB REPORT — SE 3006: Software Architecture
## Lab 01: Layered Architecture with Pure Java

| | |
|---|---|
| **Öğrenci Adı** | Utku Sina Orhan |
| **Ders Kodu** | SE 3006 — Software Architecture |
| **Lab No** | Lab 01 |
| **Tarih** | 2025 |
| **Konu** | Katmanlı Mimari (Layered Architecture) — Saf Java ile Uygulama |

---

## 1. Giriş ve Amaç

Bu laboratuvar çalışmasının temel amacı, bir yazılım sisteminin **teknik sorumluluklarına** göre yatay katmanlara nasıl ayrıldığını uygulamalı olarak gözlemlemektir.

Çalışmada Spring Boot gibi görevleri otomatikleştiren framework'ler **kullanılmamıştır.** Bunun yerine:

- Nesnelerin birbirine nasıl bağlandığı (**Manuel Dependency Injection**) gözlemlenmiştir.
- Katmanlar arasındaki katı erişim kurallarının (**Strict Layering**) kod düzeyinde nasıl uygulandığı incelenmiştir.

Bu yaklaşım, modern framework'lerin arka planda ne yaptığını anlamamızı sağlar ve mimari prensipleri temel düzeyde kavratır.

---

## 2. Mimari Tasarım

### 2.1 Katmanlı Mimari (Layered Architecture) Nedir?

Katmanlı mimari, bir sistemin farklı teknik sorumluluklarını birbirinden izole ederek yönetmeyi sağlayan bir yazılım mimarisi kalıbıdır. Her katman yalnızca bir alt katmanıyla iletişim kurar; bu sayede **bağımlılıklar tek yönlü** (yukarıdan aşağıya) akar.

### 2.2 Katman Yapısı

```
┌─────────────────────────────────────────────┐
│         PRESENTATION LAYER                  │
│         OrderController                     │
│   • Kullanıcı isteğini alır                 │
│   • Yalnızca Business Layer'ı tanır         │
└──────────────────┬──────────────────────────┘
                   │  (bağımlılık yönü: ↓)
┌──────────────────▼──────────────────────────┐
│         BUSINESS LAYER                      │
│         OrderService                        │
│   • Uygulama mantığını yönetir              │
│   • Yalnızca Persistence Layer'ı tanır      │
└──────────────────┬──────────────────────────┘
                   │  (bağımlılık yönü: ↓)
┌──────────────────▼──────────────────────────┐
│         PERSISTENCE LAYER                   │
│         ProductRepository                   │
│   • Veriyi saklar ve okur                   │
│   • Hiçbir üst katmanı tanımaz              │
└─────────────────────────────────────────────┘
```

### 2.3 Mimari Kural: Strict Layering

| Katman | Sınıf | Bağımlılığı | Bağımlı Olamaz |
|---|---|---|---|
| Presentation | `OrderController` | `OrderService` | `ProductRepository`, `Product` |
| Business | `OrderService` | `ProductRepository` | `OrderController` |
| Persistence | `ProductRepository` | `Product` (model) | Hiçbir üst katman |

---

## 3. Paket Yapısı

```
src/
└── tr/
    └── edu/
        └── mu/
            └── se3006/
                ├── Main.java                          ← Bootstrapping (Wiring)
                ├── model/
                │   └── Product.java                  ← Veri modeli
                ├── persistence/
                │   └── ProductRepository.java        ← TASK 1
                ├── business/
                │   └── OrderService.java             ← TASK 2
                └── presentation/
                    └── OrderController.java          ← TASK 3
```

---

## 4. Uygulanan Görevler (Tasks)

### TASK 1 — Persistence Layer: `ProductRepository`

**Amaç:** Ürün verilerini bellekte saklamak ve ID'ye göre erişim sağlamak.

**Yapılan İşlemler:**
- `HashMap<Long, Product>` tanımlanarak bellek içi veri deposu oluşturuldu.
- `findById(Long id)` metodu ile ID'ye göre ürün sorgusu yapılabilmesi sağlandı.
- `save(Product product)` metodu ile ürün nesnesi `HashMap`'e kaydedildi.

**Kod Özeti:**

```java
package tr.edu.mu.se3006.persistence;

import tr.edu.mu.se3006.model.Product;
import java.util.HashMap;
import java.util.Map;

public class ProductRepository {

    private final Map<Long, Product> database = new HashMap<>();

    public Product findById(Long id) {
        return database.get(id);
    }

    public void save(Product product) {
        database.put(product.getId(), product);
    }
}
```

**Katman Kuralı Kontrolü:** `ProductRepository` yalnızca `Product` modelini import etmektedir. `OrderService` veya `OrderController`'a herhangi bir bağımlılık içermemektedir. ✅

---

### TASK 2 — Business Layer: `OrderService`

**Amaç:** Sipariş iş mantığını yönetmek; stok kontrolü yaparak siparişi işlemek.

**Yapılan İşlemler:**
- `ProductRepository` bağımlılığı **constructor injection** ile enjekte edildi (Spring kullanılmadan).
- `placeOrder(Long productId, int quantity)` metodu aşağıdaki mantıkla dolduruldu:
  1. `productId` ile ürün bulundu.
  2. Stok miktarı yeterli değilse `Exception` fırlatıldı.
  3. Stok miktarı güncellendi ve ürün kaydedildi.

**Kod Özeti:**

```java
package tr.edu.mu.se3006.business;

import tr.edu.mu.se3006.model.Product;
import tr.edu.mu.se3006.persistence.ProductRepository;

public class OrderService {

    private final ProductRepository productRepository;

    public OrderService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public void placeOrder(Long productId, int quantity) throws Exception {
        Product product = productRepository.findById(productId);

        if (product == null) {
            throw new Exception("Ürün bulunamadı: ID = " + productId);
        }

        if (product.getStock() < quantity) {
            throw new Exception("Yetersiz stok! Mevcut: "
                + product.getStock() + ", İstenen: " + quantity);
        }

        product.setStock(product.getStock() - quantity);
        productRepository.save(product);
    }
}
```

**Katman Kuralı Kontrolü:** `OrderService` yalnızca `ProductRepository` ve `Product` import etmektedir. `OrderController`'dan haberdar değildir. ✅

---

### TASK 3 — Presentation Layer: `OrderController`

**Amaç:** Kullanıcı isteğini alarak iş katmanına iletmek ve sonucu kullanıcıya bildirmek.

**Yapılan İşlemler:**
- `OrderService` bağımlılığı **constructor injection** ile enjekte edildi.
- `handleUserRequest(Long productId, int quantity)` metodu `try-catch` bloğu ile yazıldı.
  - Başarı durumunda bilgilendirici mesaj yazdırıldı.
  - Hata durumunda exception mesajı kullanıcıya aktarıldı.

**Kod Özeti:**

```java
package tr.edu.mu.se3006.presentation;

import tr.edu.mu.se3006.business.OrderService;

public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    public void handleUserRequest(Long productId, int quantity) {
        try {
            orderService.placeOrder(productId, quantity);
            System.out.println("Sipariş başarıyla oluşturuldu. " +
                "Ürün ID: " + productId + ", Adet: " + quantity);
        } catch (Exception e) {
            System.out.println("Sipariş oluşturulamadı: " + e.getMessage());
        }
    }
}
```

**Katman Kuralı Kontrolü:** `OrderController` yalnızca `OrderService`'i tanımaktadır. `ProductRepository` veya `Product` sınıflarını doğrudan import etmemektedir. ✅

---

### TASK 4 — Bootstrapping: `Main` Sınıfı

**Amaç:** Tüm katmanların nesnelerini manuel olarak oluşturmak ve birbirine bağlamak (Dependency Injection olmaksızın wiring).

**Yapılan İşlemler:**
- Nesneler **aşağıdan yukarıya** sırayla oluşturuldu:
  1. `ProductRepository` oluşturuldu.
  2. `OrderService` oluşturuldu → `ProductRepository` inject edildi.
  3. `OrderController` oluşturuldu → `OrderService` inject edildi.
- Test verileri eklenerek sistem çalıştırıldı.

**Kod Özeti:**

```java
package tr.edu.mu.se3006;

import tr.edu.mu.se3006.model.Product;
import tr.edu.mu.se3006.persistence.ProductRepository;
import tr.edu.mu.se3006.business.OrderService;
import tr.edu.mu.se3006.presentation.OrderController;

public class Main {
    public static void main(String[] args) {

        // --- Bootstrapping: Aşağıdan Yukarıya Nesne Oluşturma ---

        // 1. Persistence Layer
        ProductRepository productRepository = new ProductRepository();

        // Test verisi ekle
        Product laptop = new Product(1L, "Laptop", 10);
        productRepository.save(laptop);

        // 2. Business Layer
        OrderService orderService = new OrderService(productRepository);

        // 3. Presentation Layer
        OrderController orderController = new OrderController(orderService);

        // --- Sistem Testi ---
        System.out.println("=== Test 1: Geçerli sipariş ===");
        orderController.handleUserRequest(1L, 3);

        System.out.println("\n=== Test 2: Yetersiz stok ===");
        orderController.handleUserRequest(1L, 100);

        System.out.println("\n=== Test 3: Var olmayan ürün ===");
        orderController.handleUserRequest(99L, 1);
    }
}
```

---

## 5. Sistem Akış Diyagramı

```
Kullanıcı İsteği
      │
      ▼
┌─────────────────────┐
│  OrderController    │  handleUserRequest(productId, quantity)
│  (Presentation)     │──────────────────────────────────────────►  try-catch
└────────┬────────────┘
         │ orderService.placeOrder(productId, quantity)
         ▼
┌─────────────────────┐
│   OrderService      │  1. Ürünü bul
│   (Business)        │  2. Stok kontrolü yap
└────────┬────────────┘  3. Stok güncelle & kaydet
         │ productRepository.findById(id)
         │ productRepository.save(product)
         ▼
┌─────────────────────┐
│ ProductRepository   │  HashMap üzerinden CRUD
│  (Persistence)      │
└─────────────────────┘
```

---

## 6. Test Çıktıları

Sistem `Main` sınıfı çalıştırıldığında aşağıdaki çıktılar üretilmektedir:

```
=== Test 1: Geçerli sipariş ===
Sipariş başarıyla oluşturuldu. Ürün ID: 1, Adet: 3

=== Test 2: Yetersiz stok ===
Sipariş oluşturulamadı: Yetersiz stok! Mevcut: 7, İstenen: 100

=== Test 3: Var olmayan ürün ===
Sipariş oluşturulamadı: Ürün bulunamadı: ID = 99
```

---

## 7. Mimari Prensipler — Değerlendirme

### 7.1 Separation of Concerns (SoC)

Her katman yalnızca kendi sorumluluğunu yerine getirmektedir:

| Katman | Sorumluluğu | Diğer Sorumluluklar |
|---|---|---|
| Presentation | Kullanıcı isteğini al, sonucu göster | ❌ İş mantığı içermez |
| Business | Sipariş iş kurallarını uygula | ❌ Veritabanı detaylarını bilmez |
| Persistence | Veriyi sakla ve geri getir | ❌ İş kurallarını bilmez |

### 7.2 Dependency Inversion (Manuel)

Framework kullanılmadan constructor injection uygulanmıştır. Bu yaklaşım sayesinde:
- Bağımlılıklar dışarıdan enjekte edilmekte, sınıflar kendi bağımlılıklarını `new` ile oluşturmamaktadır.
- `Main` sınıfı tek bir **composition root** görevi görmekte; tüm wiring burada gerçekleşmektedir.

### 7.3 Strict Layering Kontrolü

```
OrderController  →  OrderService      ✅ (bir alt katman)
OrderController  →  ProductRepository ❌ (katman atlamak yasak)
OrderService     →  ProductRepository ✅ (bir alt katman)
ProductRepository →  OrderService     ❌ (yukarı bağımlılık yasak)
```

---

## 8. Öğrenilen Kavramlar

| Kavram | Açıklama | Bu Lab'daki Karşılığı |
|---|---|---|
| **Layered Architecture** | Sistemi teknik sorumluluklara göre katmanlara ayırma | 3 katman: Presentation / Business / Persistence |
| **Strict Layering** | Her katman yalnızca bir alt katmanı tanır | Controller→Service→Repository zinciri |
| **Manual DI** | Bağımlılıkların constructor üzerinden dışarıdan verilmesi | `new OrderService(productRepository)` |
| **Composition Root** | Tüm nesne bağlamalarının tek bir yerde yapılması | `Main.java` sınıfı |
| **Separation of Concerns** | Her birimin yalnızca kendi işini yapması | Her katmanın tek sorumluluğu olması |

---

## 9. Sonuç

Bu laboratuvar çalışmasında, katmanlı mimarinin temel prensipleri saf Java kullanılarak uygulanmıştır. Framework'lerden bağımsız olarak:

- **Manuel Dependency Injection** ile nesneler arasındaki bağımlılıkların nasıl yönetildiği gözlemlenmiştir.
- **Strict Layering** kuralları sayesinde her katmanın yalnızca bir alt katmanına bağımlı olduğu doğrulanmıştır.
- `Main` sınıfının **Composition Root** olarak kullanılmasıyla, nesne oluşturma ve bağlama sorumluluğunun tek bir noktada toplandığı görülmüştür.

Bu uygulama, Spring gibi framework'lerin arka planda otomatikleştirdiği mekanizmaları somutlaştırarak, yazılım mimarisine dair temel anlayışı pekiştirmiştir.

---

*SE 3006 — Software Architecture | Lab 01 | Muğla Sıtkı Koçman Üniversitesi*
