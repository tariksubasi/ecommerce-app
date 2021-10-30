let images = document.getElementsByClassName("product-image");

for (let item of images) {
  let random = Math.floor(Math.random() * 100 + 100);

  item.setAttribute("src", "https://picsum.photos/" + random.toString());

  item.addEventListener("mouseover", (e) => {
    let context = this.mxcontext;

    let name = context.trackObject.jsonData.attributes.ProductName.value;
    let price = context.trackObject.jsonData.attributes.SalePrice.value;
    let id = context.trackObject.jsonData.attributes._Id.value;

    //console.warn(name, price, id);

    let popupz = document.getElementsByClassName("popup-product");

    if (popupz.length !== 0) return;

    let rootDiv = document.getElementById("content");

    rootDiv.insertAdjacentHTML(
      "afterbegin",
      `<div id = ${id} class = "popup-product"> ${name}</div>`
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
