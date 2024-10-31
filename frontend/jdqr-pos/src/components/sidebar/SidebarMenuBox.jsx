import { Stack } from '@mui/material';
import { colors } from 'constants/colors';
import { useNavigate } from 'react-router-dom';
const commonStyle = {
	fontSize: '25px',
	padding: '10px 15px 10px 15px',
	borderRadius: '5px',
	transition: 'background-color 0.3s ease, color 0.3s ease, width 0.3s ease', // 부드러운 애니메이션
	'&:hover': {
		backgroundColor: colors.background.box, // hover시 일괄적으로 회색 유지
	},
	cursor: 'pointer',
	justifyContent: 'center',
	alignItems: 'center',
};
const selectStyle = {
	backgroundColor: colors.background.box,
	fontWeight: 'bold',
	...commonStyle,
};
const nonSelectStyle = {
	...commonStyle,
};

const SidebarMenuBox = ({ text, select, path }) => {
	const navigate = useNavigate();
	return (
		<Stack
			onClick={() => {
				navigate(path);
			}}
			sx={select ? selectStyle : nonSelectStyle}>
			{text}
		</Stack>
	);
};

export default SidebarMenuBox;
