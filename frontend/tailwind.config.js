/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{vue,js,ts,jsx,tsx}'],
  theme: {
    extend: {
      colors: {
        cream: '#FFF9E6',
        'wheel-red': '#EF4444',
        'wheel-blue': '#3B82F6',
        'wheel-green': '#22C55E',
        'wheel-orange': '#F97316',
        'wheel-purple': '#A855F7',
        'wheel-pink': '#EC4899',
        challenge: '#FBBF24'
      },
      fontFamily: {
        fredoka: ['"Fredoka One"', 'cursive'],
        nunito: ['Nunito', 'sans-serif']
      }
    }
  },
  plugins: []
}
