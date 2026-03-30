import type { Config } from 'tailwindcss'

const config: Config = {
  content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
  theme: {
    extend: {
      colors: {
        primary: {
          DEFAULT: '#006947',
          dim: '#005c3d',
          container: '#69f6b8',
          fixed: '#69f6b8',
          'fixed-dim': '#58e7ab',
        },
        'on-primary': {
          DEFAULT: '#c8ffe0',
          container: '#005a3c',
          fixed: '#00452d',
          'fixed-variant': '#006544',
        },
        secondary: {
          DEFAULT: '#515c70',
          dim: '#455064',
          container: '#d8e3fb',
          fixed: '#d8e3fb',
          'fixed-dim': '#cad5ed',
        },
        'on-secondary': {
          DEFAULT: '#eff2ff',
          container: '#475266',
          fixed: '#354053',
          'fixed-variant': '#515c70',
        },
        tertiary: {
          DEFAULT: '#006575',
          dim: '#005866',
          container: '#00dcfd',
          fixed: '#00dcfd',
          'fixed-dim': '#00cdeb',
        },
        'on-tertiary': {
          DEFAULT: '#dcf7ff',
          container: '#004955',
          fixed: '#00343c',
          'fixed-variant': '#005360',
        },
        error: {
          DEFAULT: '#b31b25',
          dim: '#9f0519',
          container: '#fb5151',
        },
        'on-error': {
          DEFAULT: '#ffefee',
          container: '#570008',
        },
        surface: {
          DEFAULT: '#f5f7f9',
          dim: '#d0d5d8',
          bright: '#f5f7f9',
          variant: '#d9dde0',
          tint: '#006947',
          'container-lowest': '#ffffff',
          'container-low': '#eef1f3',
          container: '#e5e9eb',
          'container-high': '#dfe3e6',
          'container-highest': '#d9dde0',
        },
        'on-surface': {
          DEFAULT: '#2c2f31',
          variant: '#595c5e',
        },
        outline: {
          DEFAULT: '#747779',
          variant: '#abadaf',
        },
        inverse: {
          surface: '#0b0f10',
          'on-surface': '#9a9d9f',
          primary: '#69f6b8',
        },
        background: '#f5f7f9',
        'on-background': '#2c2f31',
        sidebar: '#0b0f10',
        accent: '#10B981',
      },
      fontFamily: {
        headline: ['Manrope', 'sans-serif'],
        body: ['Manrope', 'sans-serif'],
        label: ['Inter', 'sans-serif'],
      },
      borderRadius: {
        DEFAULT: '0.25rem',
        lg: '0.5rem',
        xl: '0.75rem',
        '2xl': '1rem',
        '3xl': '1.5rem',
      },
      boxShadow: {
        card: '0 20px 40px rgba(44, 47, 49, 0.06)',
        'card-hover': '0 24px 48px rgba(44, 47, 49, 0.1)',
        modal: '0 25px 50px rgba(0, 0, 0, 0.15)',
      },
      keyframes: {
        'flash-green': {
          '0%, 100%': { backgroundColor: 'transparent' },
          '50%': { backgroundColor: 'rgba(105, 246, 184, 0.3)' },
        },
        'fade-in': {
          '0%': { opacity: '0', transform: 'translateY(8px)' },
          '100%': { opacity: '1', transform: 'translateY(0)' },
        },
        'slide-up': {
          '0%': { opacity: '0', transform: 'translateY(20px)' },
          '100%': { opacity: '1', transform: 'translateY(0)' },
        },
        'scale-in': {
          '0%': { opacity: '0', transform: 'scale(0.95)' },
          '100%': { opacity: '1', transform: 'scale(1)' },
        },
      },
      animation: {
        'flash-green': 'flash-green 0.6s ease-in-out',
        'fade-in': 'fade-in 0.3s ease-out',
        'slide-up': 'slide-up 0.4s ease-out',
        'scale-in': 'scale-in 0.2s ease-out',
      },
    },
  },
  plugins: [],
}

export default config
