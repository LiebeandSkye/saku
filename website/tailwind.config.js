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
        saku: {
          matcha: '#52C47C',
          'matcha-light': '#6EE7A0',
          'matcha-dark': '#2D7D46',
          'matcha-container': '#1B3824',
          rose: '#CF7B88',
          'rose-light': '#E06C75',
          'rose-container': '#332024',
          amber: '#D9A668',
          'amber-container': '#33281D',
          slate: '#7B9EC7',
          'slate-container': '#202A36',
          bg: '#15171C',
          'bg-secondary': '#1A1D23',
          surface: '#1F222A',
          'surface-elevated': '#262A34',
          'surface-variant': '#2C313D',
          border: '#2F3440',
          'border-subtle': '#262A35',
          'border-highlight': '#414858',
          text: '#E8EAF0',
          'text-secondary': '#9AA1AD',
          'text-muted': '#6E7482',
        }
      },
      fontFamily: {
        sans: ['"Plus Jakarta Sans"', '"Noto Sans JP"', 'system-ui', 'sans-serif'],
        serif: ['"Noto Serif JP"', 'serif'],
        mono: ['"JetBrains Mono"', 'monospace'],
      },
      animation: {
        'pulse-subtle': 'pulseSubtle 3s cubic-bezier(0.4, 0, 0.6, 1) infinite',
        'float': 'float 6s ease-in-out infinite',
      },
      keyframes: {
        pulseSubtle: {
          '0%, 100%': { opacity: 0.95 },
          '50%': { opacity: 0.6 },
        },
        float: {
          '0%, 100%': { transform: 'translateY(0px)' },
          '50%': { transform: 'translateY(-8px)' },
        }
      },
      boxShadow: {
        'saku-card': '0 10px 30px -10px rgba(0, 0, 0, 0.5), 0 0 1px 1px rgba(255, 255, 255, 0.05)',
        'saku-glow': '0 0 25px -5px rgba(82, 196, 124, 0.25)',
        'saku-rose-glow': '0 0 25px -5px rgba(207, 123, 136, 0.25)',
      }
    },
  },
  plugins: [],
}
