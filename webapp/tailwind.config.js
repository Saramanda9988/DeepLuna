import daisyui from 'daisyui'
import scrollbarHide from 'tailwind-scrollbar-hide'
import typography from '@tailwindcss/typography'

/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
  theme: {
    extend: {},
  },
  plugins: [daisyui, scrollbarHide, typography],
  daisyui: {
    themes: ['light', 'dark'],
  },
}
