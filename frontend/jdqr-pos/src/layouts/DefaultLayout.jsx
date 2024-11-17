import React, { useEffect } from 'react';
import { Stack, Box } from '@mui/material';
import Sidebar from 'components/sidebar/Sidebar';
import { useLocation, useNavigate, Outlet } from 'react-router-dom';
import HeaderBox from 'components/header/HeaderBox';
import { fetchRestaurant } from 'utils/apis/setting';
const DefaultLayout = () => {
	useEffect(() => {
		fetchRestaurant().then(response => {
			sessionStorage.setItem(
				'restaurantInfo',
				JSON.stringify(response.data)
			);
		});
	}, []);
	return (
		<Stack direction="column" sx={{ width: '100vw', height: '100vh' }}>
			{/* 헤더 */}
			<HeaderBox />

			<Stack direction="row" sx={{ width: '100%', height: '100%' }}>
				{/* 좌측 사이드바 */}
				<Sidebar />
				<Outlet />
			</Stack>
		</Stack>
	);
};

export default DefaultLayout;
