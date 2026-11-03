/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        'bg-color': '#f8f9fa',
        'card-bg': '#ffffff',
        'text-primary': '#2c3e50',
        'text-secondary': '#7f8c8d',
        'accent-color': '#ff6b6b',
        'accent-hover': '#ff5252',
        'message-me': '#e3f2fd',
        'message-other': '#ff6b6b',
        'border-color': '#e9ecef',
        'outer-bg': '#e0e0e0',
      },
      spacing: {
        'nav-height': '60px',
      },
      fontFamily: {
        sans: ['-apple-system', 'BlinkMacSystemFont', 'Segoe UI', 'Roboto', 'Helvetica', 'Arial', 'sans-serif'],
      },
    },
  },
  plugins: [],
}