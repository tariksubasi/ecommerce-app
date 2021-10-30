let images = document.getElementsByClassName("product-image");

for (let item of images) {
  let random = Math.floor(Math.random() * 100 + 100);

  item.setAttribute("src", "https://picsum.photos/" + random.toString());

  item.addEventListener("mouseover", (e) => {
    let context = this.mxcontext;

    let name = context.trackObject.jsonData.attributes.ProductName.value;
    let price = context.trackObject.jsonData.attributes.SalePrice.value;
    let id = context.trackObject.jsonData.attributes._Id.value;
    //let category = context.trackObject.jsonData.attributes.ProductCategory.value;
    console.warn(context);

    //console.warn(name, price, id);

    let popupz = document.getElementsByClassName("popup-product");

    if (popupz.length !== 0) return;

    let rootDiv = document.getElementById("content");

    rootDiv.insertAdjacentHTML(
      "afterbegin",
      `<div id = ${id} class = "popup-product"> 
      <div class="product-card">
      <div class="badge">Hot</div>    
      <div class="product-details">
          <span class="product-catagory">Product Category</span>
          <h4><a href="">${name}</a></h4>
          <p>Lorem ipsum dolor sit amet, consectetur adipisicing elit. Vero, possimus nostrum!</p>
          <div class="product-bottom-details">
              <div class="product-price"><small>${Math.floor(
                Math.random() * 1000
              ).toString()}$</small>${price}$</div>
              <div class="product-links">
                  <a href=""><i class="fa fa-heart"></i></a>
                  <a href=""><i class="fa fa-shopping-cart"></i></a>
              </div>
          </div>
      </div>
  </div>
      </div>`
    );

    let popup = document.getElementById(id);

    popup.style.top = e.clientY + "px";
    popup.style.left = e.clientX + "px";
  });

  item.addEventListener("mouseleave", (e) => {
    let context = this.mxcontext;
    let id = context.trackObject.jsonData.attributes._Id.value;
    let popup = document.getElementById(id);
    if (!popup) return;
    popup.remove();
  });
}
