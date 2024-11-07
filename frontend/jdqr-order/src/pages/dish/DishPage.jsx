import { Box, Stack } from "@mui/material";
import Header from "../../components/header/Header";
import DishHeader from "./DishHeader";
import DishList from "./DishList";

const DishPage = () => {
  const mockData = {
    status: 200,
    message: "메뉴 조회에 성공하였습니다.",
    data: {
      tableId: "67213b600b750d07ba21805b",
      tableName: "영표의 식탁",
      peopleCount: 7,
      categories: ["인기 메뉴", "피자", "사이드", "음료/주류"],
      dishes: [
        {
          categoryId: 1,
          categoryName: "인기 메뉴",
          items: [
            {
              dishId: 5,
              dishName: "치즈볼",
              price: 3000,
              description: "쫄깃하고 고소한 치즈볼",
              imageUrl: "https://example.com/image5.jpg",
              tags: [],
            },
          ],
        },
        {
          categoryId: 2,
          categoryName: "피자",
          items: [
            {
              dishId: 1,
              dishName: "핫치킨 피자",
              price: 12800,
              description: "불닭볶음면보다 매운 피자🔥",
              imageUrl: "https://example.com/image1.jpg",
              tags: ["인기", "시그니처"],
            },
            {
              dishId: 2,
              dishName: "핫치킨 피자",
              price: 12800,
              description: "불닭볶음면보다 매운 피자🔥",
              imageUrl: "https://example.com/image2.jpg",
              tags: ["시그니처"],

              options: [
                {
                  optionId: 1,
                  optionName: "도우 변경",
                  choices: [
                    {
                      choiceId: 1,
                      choiceName: "치즈 추가",
                      price: 2000,
                    },
                    {
                      choiceId: 2,
                      choiceName: "고구마 무스 추가",
                      price: 2000,
                    },
                    {
                      choiceId: 3,
                      choiceName: "치즈 크러스트로 변경",
                      price: 4000,
                    },
                    {
                      choiceId: 4,
                      choiceName: "골드 크러스트로 변경",
                      price: 5000,
                    },
                  ],
                },
              ],
            },
            {
              dishId: 3,
              dishName: "핫치킨 피자",
              price: 12800,
              description: "불닭볶음면보다 매운 피자🔥",
              imageUrl: "https://example.com/image3.jpg",
              tags: ["한정"],
            },
            {
              dishId: 4,
              dishName: "핫치킨 피자",
              price: 12800,
              description: "불닭볶음면보다 매운 피자🔥",
              imageUrl: "https://example.com/image4.jpg",
              tags: [],
            },
          ],
        },
        {
          categoryId: 3,
          categoryName: "사이드",
          items: [
            {
              dishId: 5,
              dishName: "치즈볼",
              price: 3000,
              description: "쫄깃하고 고소한 치즈볼",
              imageUrl: "https://example.com/image5.jpg",
              tags: [],
            },
          ],
        },
      ],
    },
  };

  return (
    <Box
      sx={{
        height: "100vh",
        overflowY: "auto",
        "&::-webkit-scrollbar": {
          display: "none",
        },
        msOverflowStyle: "none",
        scrollbarWidth: "none",
      }}
    >
      <Header title={mockData.data.tableName} />
      <DishHeader />
      <DishList dishes={mockData.data.dishes} />
    </Box>
  );
};

export default DishPage;