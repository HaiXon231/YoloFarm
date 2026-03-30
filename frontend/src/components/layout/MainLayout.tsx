import { Outlet } from 'react-router-dom'
import Sidebar from './Sidebar'
import TopHeader from './TopHeader'

export default function MainLayout() {
  return (
    <div className="min-h-screen bg-background">
      <Sidebar />
      <TopHeader />
      <main className="ml-64 pt-24 p-8 min-h-screen">
        <Outlet />
      </main>
    </div>
  )
}
