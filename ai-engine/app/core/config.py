from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    app_name: str = "smartreport-ai-engine"
    llm_provider: str = "mock"
    deepseek_api_key: str = ""
    openai_api_key: str = ""
    zhipu_api_key: str = ""


settings = Settings()
