import daisyui from 'daisyui'
import scrollbarHide from 'tailwind-scrollbar-hide'

/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
  theme: {
    extend: {},
  },
  plugins: [daisyui, scrollbarHide],
  daisyui: {
    themes: ['light', 'dark'],
  },
}
