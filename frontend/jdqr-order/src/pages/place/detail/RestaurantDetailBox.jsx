import React from "react"
import RestaurantInfo from "./RestaurantInfo"
import MapListContainer from "../../../components/container/MapListContainer"

const RestaurantDetailBox = () => {
  return (
    <MapListContainer
      sx={{
        backgroundColor: "white",
        height: "50vh",
      }}
    >
      <RestaurantInfo />
    </MapListContainer>
  )
}

export default RestaurantDetailBox