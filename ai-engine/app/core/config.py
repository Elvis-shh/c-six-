from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    app_name: str = "smartreport-ai-engine"
    llm_provider: str = "mock"
    deepseek_api_key: str = ""
    deepseek_base_url: str = "https://api.deepseek.com/v1"
    deepseek_model: str = "deepseek-chat"
    openai_api_key: str = ""
    zhipu_api_key: str = ""


settings = Settings()
