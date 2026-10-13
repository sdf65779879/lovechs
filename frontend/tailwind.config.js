/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{vue,js,ts,jsx,tsx}'],
  theme: {
    extend: {
      colors: {
        // 奶白底
        cream: {
          50: '#FFFDFB',
          100: '#FFF9F4',
          200: '#FDF1E7',
          300: '#F9E4D2',
        },
        // 奶油橘主色
        primary: {
          50: '#FFF3EE',
          100: '#FFE3D6',
          200: '#FFC7B0',
          300: '#FFAB8A',
          400: '#FF8A65',
          500: '#FF7A4D',
          600: '#F26438',
          700: '#D8502A',
        },
        // 草莓粉
        berry: {
          50: '#FFF4F6',
          100: '#FFE1E7',
          200: '#FFC2CD',
          300: '#FF9DAE',
          400: '#FF6B81',
          500: '#F8556E',
          700: '#A6203E',
        },
        // 蜂蜜黄
        honey: {
          200: '#FFE7B0',
          300: '#FFD580',
          400: '#FFB74D',
          500: '#F5A623',
        },
        // 暖棕文字
        cocoa: {
          300: '#A1887F',
          500: '#8D6E63',
          700: '#6D4C41',
          900: '#4E342E',
        },
      },
      fontFamily: {
        sans: [
          '"PingFang SC"',
          '"Hiragino Sans GB"',
          '"Microsoft YaHei"',
          '"Helvetica Neue"',
          'Helvetica',
          'Arial',
          'sans-serif',
        ],
        round: ['"Comic Sans MS"', '"Chalkboard SE"', '"PingFang SC"', 'sans-serif'],
      },
      borderRadius: {
        soft: '14px',
        pill: '999px',
      },
      boxShadow: {
        warm: '0 8px 24px -8px rgba(255, 138, 101, 0.45)',
        card: '0 6px 20px -10px rgba(109, 76, 65, 0.25)',
        float: '0 10px 30px -8px rgba(255, 107, 129, 0.5)',
      },
      keyframes: {
        'pop-in': {
          '0%': { transform: 'scale(0.9)', opacity: '0' },
          '100%': { transform: 'scale(1)', opacity: '1' },
        },
        'slide-up': {
          '0%': { transform: 'translateY(24px)', opacity: '0' },
          '100%': { transform: 'translateY(0)', opacity: '1' },
        },
        'soft-bounce': {
          '0%, 100%': { transform: 'translateY(0)' },
          '50%': { transform: 'translateY(-6px)' },
        },
        'heartbeat': {
          '0%, 100%': { transform: 'scale(1)' },
          '25%': { transform: 'scale(1.15)' },
          '45%': { transform: 'scale(0.95)' },
        },
      },
      animation: {
        'pop-in': 'pop-in 0.25s ease-out',
        'slide-up': 'slide-up 0.3s ease-out',
        'soft-bounce': 'soft-bounce 1.6s ease-in-out infinite',
        heartbeat: 'heartbeat 1.2s ease-in-out infinite',
      },
    },
  },
  plugins: [],
}
