/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{vue,js,ts,jsx,tsx}'],
  theme: {
    extend: {
      colors: {
        // CrumbleMagic brand palette
        primary: {
          DEFAULT: '#C59F71', // warm caramel
          50: '#FAF3EA',
          100: '#F2E4D0',
          200: '#E5C9A4',
          300: '#D9B085',
          400: '#C59F71',
          500: '#B0855A',
          600: '#9A6E45',
          700: '#7E5836',
          800: '#5F4128',
          900: '#3F2B1B',
        },
        chocolate: {
          DEFAULT: '#2E0E0B', // dark chocolate
          50: '#7A4A3F',
          100: '#5E352D',
          200: '#4D2A22',
          300: '#3D1F17',
          400: '#351710',
          500: '#2E0E0B',
          600: '#260A08',
          700: '#1E0805',
          800: '#160503',
          900: '#0E0302',
        },
        cream: {
          DEFAULT: '#E2D9C8',
          50: '#FBF9F4',
          100: '#F5F0E6',
          200: '#EDE5D5',
          300: '#E2D9C8',
          400: '#D2C5AC',
          500: '#BBA98A',
        },
        background: '#FAF8F4',
        surface: '#FFFFFF',
      },
      fontFamily: {
        sans: ['Outfit', 'ui-sans-serif', 'system-ui', 'sans-serif'],
        heading: ['Outfit', 'ui-sans-serif', 'system-ui', 'sans-serif'],
      },
      fontSize: {
        // Typography scale per spec
        h1: ['3.25rem', { lineHeight: '1.1', fontWeight: '500' }], // 52px
        h2: ['3rem', { lineHeight: '1.15', fontWeight: '400' }], // 48px
        body: ['1.25rem', { lineHeight: '1.7', fontWeight: '400' }], // 20px
        card: ['1rem', { lineHeight: '1.6', fontWeight: '400' }], // 16px
      },
      borderRadius: {
        '4xl': '2rem',
        '5xl': '2.5rem',
      },
      boxShadow: {
        soft: '0 4px 20px -2px rgba(46, 14, 11, 0.08)',
        'soft-lg': '0 12px 40px -4px rgba(46, 14, 11, 0.12)',
        'soft-xl': '0 24px 60px -8px rgba(46, 14, 11, 0.18)',
        glow: '0 0 40px -8px rgba(197, 159, 113, 0.45)',
        cookie: '0 20px 50px -10px rgba(46, 14, 11, 0.35)',
      },
      backgroundImage: {
        'cookie-gradient': 'linear-gradient(135deg, #C59F71 0%, #B0855A 100%)',
        'chocolate-gradient': 'linear-gradient(160deg, #3D1F17 0%, #2E0E0B 60%, #1E0805 100%)',
        'cream-gradient': 'linear-gradient(180deg, #FAF8F4 0%, #F2EAD9 100%)',
      },
      keyframes: {
        'fade-in': {
          '0%': { opacity: '0' },
          '100%': { opacity: '1' },
        },
        'fade-up': {
          '0%': { opacity: '0', transform: 'translateY(24px)' },
          '100%': { opacity: '1', transform: 'translateY(0)' },
        },
        'fade-down': {
          '0%': { opacity: '0', transform: 'translateY(-24px)' },
          '100%': { opacity: '1', transform: 'translateY(0)' },
        },
        'scale-in': {
          '0%': { opacity: '0', transform: 'scale(0.95)' },
          '100%': { opacity: '1', transform: 'scale(1)' },
        },
        float: {
          '0%, 100%': { transform: 'translateY(0)' },
          '50%': { transform: 'translateY(-12px)' },
        },
        'spin-slow': {
          '0%': { transform: 'rotate(0deg)' },
          '100%': { transform: 'rotate(360deg)' },
        },
        shimmer: {
          '0%': { backgroundPosition: '-1000px 0' },
          '100%': { backgroundPosition: '1000px 0' },
        },
        ripple: {
          '0%': { transform: 'scale(0)', opacity: '0.6' },
          '100%': { transform: 'scale(4)', opacity: '0' },
        },
      },
      animation: {
        'fade-in': 'fade-in 0.6s ease-out both',
        'fade-up': 'fade-up 0.7s cubic-bezier(0.22, 1, 0.36, 1) both',
        'fade-down': 'fade-down 0.7s cubic-bezier(0.22, 1, 0.36, 1) both',
        'scale-in': 'scale-in 0.5s cubic-bezier(0.22, 1, 0.36, 1) both',
        float: 'float 6s ease-in-out infinite',
        'spin-slow': 'spin-slow 20s linear infinite',
        shimmer: 'shimmer 2s linear infinite',
        ripple: 'ripple 0.6s ease-out',
      },
      transitionTimingFunction: {
        'cookie': 'cubic-bezier(0.22, 1, 0.36, 1)',
      },
    },
  },
  plugins: [],
}
