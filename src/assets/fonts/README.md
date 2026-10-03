# Fuentes / Fonts

En el prototipo v1 la fuente Inter se carga desde Google Fonts CDN mediante `@import`
en `src/css/styles.css`.

Para producción (v2+), descargar los archivos `.woff2` de Inter y colocarlos aquí,
luego reemplazar el `@import` por bloques `@font-face` locales:

```css
@font-face {
  font-family: 'Inter';
  font-style: normal;
  font-weight: 400;
  font-display: swap;
  src: url('../assets/fonts/inter-400.woff2') format('woff2');
}
@font-face {
  font-family: 'Inter';
  font-style: normal;
  font-weight: 500;
  font-display: swap;
  src: url('../assets/fonts/inter-500.woff2') format('woff2');
}
@font-face {
  font-family: 'Inter';
  font-style: normal;
  font-weight: 600;
  font-display: swap;
  src: url('../assets/fonts/inter-600.woff2') format('woff2');
}
@font-face {
  font-family: 'Inter';
  font-style: normal;
  font-weight: 700;
  font-display: swap;
  src: url('../assets/fonts/inter-700.woff2') format('woff2');
}
```

Fuente de descarga: https://fonts.google.com/specimen/Inter
Pesos necesarios: 400, 500, 600, 700.
