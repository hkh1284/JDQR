import { useState } from 'react';
import { Stack, Typography, Box, Switch, Container } from '@mui/material';
import PageTitleBox from 'components/title/PageTitleBox';
import OrderBox from './order/OrderBox';
import { tableData } from 'sampleData/apiMock';
import OrderDetailBox from './order/OrderDetailBox';
import FlatButton from 'components/button/FlatButton';
import { colors } from 'constants/colors';
import TableSettingGridBox from './table/TableSettingGridBox';
import TableGridBox from './table/TableGridBox';
import TableEditBox from './table/edit/TableEditBox';
import QRCodeSettingDialog from './qr/QRCodeSettingDialog';

const SampleTableRequestData = tableData;

const TablePage = () => {
	const rightMenuPreset = {
		ORDER: 'order',
		ADD: 'add',
		EDIT: 'edit',
	};
	Object.freeze(rightMenuPreset);
	const [selectedTable, setSelectedTable] = useState(
		SampleTableRequestData.data.tables[0]
	);
	const handleTableClick = table => {
		setrightMenu(rightMenuPreset.ORDER);
		setSelectedTable(table);
	};
	const [tableViewChecked, setTableViewChecked] = useState(false);

	const handleTableViewCheckedChange = event => {
		setTableViewChecked(event.target.checked);
	};

	const [rightMenu, setrightMenu] = useState('order');

	const [open, setOpen] = useState(false);

	const handleClickOpen = () => {
		setOpen(true);
	};

	const handleClose = () => {
		setOpen(false);
	};

	const addNewTable = () => {};
	const editCurrentTable = () => {};
	return (
		<Stack
			direction="column"
			sx={{ padding: '25px', width: '100%' }}
			spacing={2}>
			{/* 페이지 상단 메뉴바 */}
			<Stack
				direction="row"
				spacing={2}
				sx={{ justifyContent: 'space-between', width: '100%' }}>
				<Stack
					spacing={1}
					direction="row"
					sx={{ justifyContent: 'center', alignItems: 'center' }}>
					<PageTitleBox title="주문 상태" />
					<Typography sx={{ fontSize: '25px', fontWeight: '600' }}>
						{'테이블로 보기'}
					</Typography>
					<Switch
						checked={tableViewChecked}
						onChange={handleTableViewCheckedChange}
						sx={{ transform: 'scale(1.5)' }}
					/>
				</Stack>
				<FlatButton
					text="테이블 추가"
					fontColor={colors.main.primary700}
					color={colors.main.primary100}
					onClick={() => {
						setrightMenu(rightMenuPreset.ADD);
					}}
					sx={{ marginLeft: 'auto' }} // 오른쪽으로 치우침
				/>
			</Stack>
			<Stack
				direction="row"
				spacing={3}
				sx={{ width: '100%', height: '100%' }}>
				{/* 테이블 정보 리스트 */}
				{!tableViewChecked && (
					<Stack
						direction="row"
						spacing={2}
						useFlexGap
						sx={{
							justifyContent: 'flex-start',
							alignItems: 'flex-start',
							width: '100%', // 원하는 너비로 설정
							flexWrap: 'wrap',
							height: 'fit-content',
						}}>
						{SampleTableRequestData.data.tables.map(table => (
							<Box
								key={table.name}
								onClick={() => {
									handleTableClick(table);
								}}>
								<OrderBox
									tableName={`${table.name} (${table.people}인)`}
									order={table.dishes}
									color={table.color}
								/>
							</Box>
						))}
					</Stack>
				)}
				{tableViewChecked && (
					<TableSettingGridBox
						tables={SampleTableRequestData.data.tables}
					/>
				)}
				{/* 테이블 상세 주문 정보 */}
				<Stack
					spacing={1}
					sx={{ height: '100%', justifyContent: 'space-between' }}>
					{rightMenu === rightMenuPreset.ORDER && (
						<OrderDetailBox table={selectedTable} />
					)}
					{rightMenu === rightMenuPreset.ADD && (
						<TableEditBox isEdit={false} />
					)}
					{rightMenu === rightMenuPreset.EDIT && (
						<TableEditBox table={selectedTable} isEdit={true} />
					)}
					<Stack spacing={1}>
						<FlatButton
							text="QR 보기"
							onClick={() => {
								handleClickOpen();
							}}
							color={colors.point.blue}
						/>

						{rightMenu === rightMenuPreset.EDIT && (
							<FlatButton
								text="테이블 저장"
								color={colors.point.red}
								onClick={() => {
									editCurrentTable();
								}}
							/>
						)}

						{rightMenu === rightMenuPreset.ADD && (
							<FlatButton
								text="테이블 저장"
								color={colors.point.red}
								onClick={() => {
									addNewTable();
								}}
							/>
						)}

						{rightMenu === rightMenuPreset.ORDER && (
							<FlatButton
								text="테이블 수정"
								color={colors.point.red}
								onClick={() => {
									setrightMenu(rightMenuPreset.EDIT);
								}}
							/>
						)}
					</Stack>
				</Stack>
			</Stack>
			<QRCodeSettingDialog
				table={selectedTable}
				open={open}
				onClose={handleClose}
			/>
		</Stack>
	);
};

export default TablePage;